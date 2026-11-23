package com.indocrm.indocrm.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.Call;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.CallRepository;
import com.indocrm.indocrm.repository.LeadRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;

/**
 * PERFORMANCE FIX
 * ---------------
 * The old DashboardService did leadRepository.findAll() + callRepository.findAll()
 * (whole collections, including the heavy excelData / activityHistory fields) and then
 * counted everything in Java using reflection. The first dashboard load after every
 * cache expiry therefore took many seconds.
 *
 * This version asks MongoDB to do the counting (count queries + one aggregation) and only
 * loads small projected documents for the last-7-days chart and "recent calls".
 *
 * It also fixes the telecaller scope: a telecaller's leads are the leads of his ASSIGNED
 * DISTRICTS (same rule as My Leads / Call Panel), not Lead.assignedTo == username, which
 * is why "My Leads" on the dashboard showed 0.
 *
 * The JSON shape is unchanged (+ the extra "today" keys Dashboard.jsx already reads).
 */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    private static final long RESPONSE_TTL_MS = 20_000L;

    private final LeadRepository leadRepository;
    private final CallRepository callRepository;
    private final TelecallerRepository telecallerRepository;
    private final MongoTemplate mongo;

    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();

    public DashboardService(LeadRepository leadRepository,
                            CallRepository callRepository,
                            TelecallerRepository telecallerRepository,
                            MongoTemplate mongo) {
        this.leadRepository = leadRepository;
        this.callRepository = callRepository;
        this.telecallerRepository = telecallerRepository;
        this.mongo = mongo;
    }

    private static class CachedResponse {
        final long at;
        final Map<String, Object> data;
        CachedResponse(long at, Map<String, Object> data) { this.at = at; this.data = data; }
    }

    // ───────────────────────────────────────────────────────── public API
    public Map<String, Object> getDashboard() {
        String username = getCurrentUsername();
        String role = getCurrentRole();
        String cacheKey = (username == null ? "anon" : username) + "|" + (role == null ? "" : role);

        long now = System.currentTimeMillis();
        CachedResponse cached = responseCache.get(cacheKey);
        if (cached != null && now - cached.at < RESPONSE_TTL_MS) return cached.data;

        Map<String, Object> fresh = buildDashboard(username, role);
        responseCache.put(cacheKey, new CachedResponse(now, fresh));
        responseCache.entrySet().removeIf(e -> now - e.getValue().at > RESPONSE_TTL_MS * 3);
        return fresh;
    }

    // ───────────────────────────────────────────────────────── admin dashboard (real data for AdminDashboard.jsx)
    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAdminDashboard() {
        String cacheKey = "admin-page|" + getCurrentUsername();
        long now = System.currentTimeMillis();
        CachedResponse cached = responseCache.get(cacheKey);
        if (cached != null && now - cached.at < RESPONSE_TTL_MS) return cached.data;

        Map<String, Object> out = new LinkedHashMap<>(getDashboard());
        Map<String, Object> summary = new LinkedHashMap<>((Map<String, Object>) out.get("summary"));

        // ---- telecallers ----
        long totalTc = 0, activeTc = 0;
        for (Telecaller tc : telecallerRepository.findAll()) {
            String role = tc.getRole() == null ? "" : tc.getRole().toUpperCase(Locale.ROOT);
            if (role.contains("ADMIN")) continue;
            totalTc++;
            String st = tc.getStatus() == null ? "" : tc.getStatus().trim();
            if (st.isEmpty() || st.equalsIgnoreCase("Active")) activeTc++;
        }
        summary.put("totalTelecallers", totalTc);
        summary.put("activeTelecallers", activeTc);
        summary.put("inactiveTelecallers", totalTc - activeTc);

        // ---- agreements signed (approved / sent to client) ----
        summary.put("agreementsSigned", mongo.count(new Query(Criteria.where("status").in("APPROVED", "SENT")), "agreements"));

        // ---- verified payments: revenue + monthly sales + district revenue ----
        Query pq = new Query(verifiedPaymentCriteria());
        pq.fields().include("amountPaid").include("totalAmount").include("paymentDate")
                .include("verifiedAt").include("createdAt").include("leadId");
        List<Document> payments = mongo.find(pq, Document.class, "payments");

        double revenue = 0;
        Map<String, Double> monthly = new java.util.TreeMap<>();       // yyyy-MM -> collected
        Map<String, Double> byLead = new HashMap<>();
        for (Document p : payments) {
            double paid = num(p.get("amountPaid"));
            double total = num(p.get("totalAmount"));
            revenue += total > 0 ? total : paid;
            String ym = paymentMonth(p);
            if (ym != null) monthly.merge(ym, paid, Double::sum);
            Object lid = p.get("leadId");
            if (lid != null) byLead.merge(String.valueOf(lid), paid, Double::sum);
        }
        summary.put("revenue", revenue);
        List<Map<String, Object>> monthlySales = new ArrayList<>();
        monthly.forEach((ym, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year", Integer.parseInt(ym.substring(0, 4)));
            m.put("month", Integer.parseInt(ym.substring(5, 7)));
            m.put("sales", v);
            monthlySales.add(m);
        });
        out.put("monthlySales", monthlySales);

        // ---- this week's calls (Mon..Sun) ----
        LocalDate monday = LocalDate.now(ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long[] byDay = new long[7];
        for (Call c : fetchCallsSince(null, Date.from(monday.atStartOfDay(ZONE).toInstant()))) {
            Date d = callDate(c);
            if (d == null) continue;
            long idx = ChronoUnit.DAYS.between(monday, d.toInstant().atZone(ZONE).toLocalDate());
            if (idx >= 0 && idx < 7) byDay[(int) idx]++;
        }
        List<Map<String, Object>> daily = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day", DAYS[i]);
            m.put("calls", byDay[i]);
            daily.add(m);
        }
        out.put("dailyCalls", daily);

        // ---- district + category performance (leads grouped by field + status) ----
        Map<String, Object> districtRevenueByName = new HashMap<>();
        if (!byLead.isEmpty()) {
            Query lq = new Query(Criteria.where("leadId").in(byLead.keySet()));
            lq.fields().include("leadId").include("district");
            Map<String, Double> rev = new HashMap<>();
            for (Lead l : mongo.find(lq, Lead.class)) {
                Double v = byLead.get(l.getLeadId());
                if (v == null) continue;
                rev.merge(blankTo(l.getDistrict(), "Unassigned"), v, Double::sum);
            }
            rev.forEach(districtRevenueByName::put);
        }
        out.put("districtPerformance", groupPerformance("district", "Unassigned", districtRevenueByName));
        out.put("categoryPerformance", groupPerformance("category", "Other", null));

        out.put("summary", summary);
        responseCache.put(cacheKey, new CachedResponse(now, out));
        return out;
    }

    private List<Map<String, Object>> groupPerformance(String field, String fallback, Map<String, Object> revenue) {
        AggregationResults<Document> res = mongo.aggregate(
                Aggregation.newAggregation(Aggregation.group(field, "status").count().as("count")),
                "leads", Document.class);
        Map<String, long[]> rows = new LinkedHashMap<>();    // [leads, converted]
        for (Document d : res.getMappedResults()) {
            Document id = (Document) d.get("_id");
            String name = blankTo(id == null || id.get(field) == null ? null : String.valueOf(id.get(field)), fallback);
            String st = id == null || id.get("status") == null ? "" : String.valueOf(id.get("status")).toLowerCase(Locale.ROOT);
            long c = ((Number) d.get("count")).longValue();
            long[] row = rows.computeIfAbsent(name, k -> new long[2]);
            row[0] += c;
            if (st.contains("converted") || st.contains("client") || st.contains("closed won")) row[1] += c;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        rows.forEach((name, r) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(field, name);
            m.put("leads", r[0]);
            m.put("converted", r[1]);
            m.put("rate", r[0] == 0 ? 0 : round(r[1] * 100.0 / r[0]));
            if (revenue != null) m.put("revenue", revenue.get(name) == null ? 0.0 : revenue.get(name));
            list.add(m);
        });
        list.sort((a, b) -> Long.compare((Long) b.get("leads"), (Long) a.get("leads")));
        return list;
    }

    private static String blankTo(String v, String fallback) { return v == null || v.isBlank() ? fallback : v.trim(); }

    private static double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0.0; }

    /** yyyy-MM of the payment: paymentDate (yyyy-MM-dd) first, else verifiedAt, else createdAt. */
    private String paymentMonth(Document p) {
        Object pd = p.get("paymentDate");
        if (pd != null && String.valueOf(pd).matches("^\\d{4}-\\d{2}.*")) return String.valueOf(pd).substring(0, 7);
        Object d = p.get("verifiedAt") != null ? p.get("verifiedAt") : p.get("createdAt");
        if (d instanceof Date dt) {
            LocalDate ld = dt.toInstant().atZone(ZONE).toLocalDate();
            return String.format("%04d-%02d", ld.getYear(), ld.getMonthValue());
        }
        return null;
    }

    // ───────────────────────────────────────────────────────── builder
    private Map<String, Object> buildDashboard(String username, String role) {
        boolean admin = isAdmin(role);
        Criteria leadScope = leadScope(username, role, admin);   // null = everything (admin)
        Criteria callScope = admin ? null : Criteria.where("agentUsername").is(username == null ? "" : username);

        Date todayStart = Date.from(LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant());
        Date weekStart = Date.from(LocalDate.now(ZONE).minusDays(6).atStartOfDay(ZONE).toInstant());

        // ---- lead status counts (ONE aggregation) ----
        Map<String, Long> statusCounts = groupCount("leads", leadScope, "status");

        long totalLeads = 0, newLeads = 0, interestedLeads = 0, convertedLeads = 0;
        for (Map.Entry<String, Long> e : statusCounts.entrySet()) {
            String s = e.getKey() == null ? "" : e.getKey().trim().toLowerCase(Locale.ROOT);
            long c = e.getValue();
            totalLeads += c;
            if (s.isEmpty() || s.contains("new")) newLeads += c;
            if (s.equals("interested")) interestedLeads += c;
            if (s.contains("converted") || s.contains("client") || s.contains("closed won")) convertedLeads += c;
        }

        long followUps = countLeads(leadScope, followUpCriteria());

        // ---- calls (count queries, no documents loaded) ----
        long callsMade = countCalls(callScope, null);
        long connectedCalls = countCalls(callScope, connectedCriteria());
        long missedCalls = countCalls(callScope, missedCriteria());

        double conversionRate = totalLeads == 0 ? 0 : round((convertedLeads * 100.0) / totalLeads);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalLeads", totalLeads);
        summary.put("assignedLeads", totalLeads);
        summary.put("newLeads", newLeads);
        summary.put("interestedLeads", interestedLeads);
        summary.put("clientsConverted", convertedLeads);
        summary.put("followUps", followUps);
        summary.put("callsMade", callsMade);
        summary.put("connectedCalls", connectedCalls);
        summary.put("missedCalls", missedCalls);
        summary.put("conversionRate", conversionRate);
        summary.put("paymentsCollected", sumVerifiedPayments(username, admin, null));
        summary.put("quotations", countOwned("quotations", username, admin, Criteria.where("status").ne("Draft")));
        summary.put("agreements", countOwned("agreements", username, admin, Criteria.where("status").ne("DRAFT")));
        if (admin) {
            summary.put("totalTelecallers", telecallerRepository.count());
            summary.put("overallLeads", leadRepository.count());
            summary.put("overallCalls", callRepository.count());
        }

        // ---- last 7 days of calls / leads (small projected docs) ----
        List<Call> weekCalls = fetchCallsSince(callScope, weekStart);
        List<Lead> weekLeads = fetchLeadDatesSince(leadScope, weekStart);

        // ---- today ----
        long todayCalls = 0, todayConnected = 0, todayMissed = 0;
        for (Call c : weekCalls) {
            if (!isToday(callDate(c))) continue;
            todayCalls++;
            if (isConnected(c)) todayConnected++;
            else if (isMissed(c)) todayMissed++;
        }
        long todayNewLeads = countLeads(leadScope, Criteria.where("createdAt").gte(todayStart));
        String todayIso = LocalDate.now(ZONE).toString();
        long todayFollowUps = countLeads(leadScope,
                Criteria.where("tcNextRefollowupDate").regex("^" + Pattern.quote(todayIso)));
        long todayInterested = countLeads(leadScope, new Criteria().andOperator(
                Criteria.where("status").regex("^interested$", "i"),
                Criteria.where("updatedAt").gte(todayStart)));
        long todayConverted = countLeads(leadScope, new Criteria().andOperator(
                Criteria.where("status").regex("converted|client|closed won", "i"),
                Criteria.where("updatedAt").gte(todayStart)));

        Map<String, Object> today = new LinkedHashMap<>();
        today.put("date", todayIso);
        today.put("callsMade", todayCalls);
        today.put("connectedCalls", todayConnected);
        today.put("missedCalls", todayMissed);
        today.put("newLeads", todayNewLeads);
        today.put("newLeadsContacted", todayNewLeads);
        today.put("followUps", todayFollowUps);
        today.put("followUpsCompleted", todayFollowUps);
        today.put("interestedLeads", todayInterested);
        today.put("clientsConverted", todayConverted);
        today.put("quotations", countOwned("quotations", username, admin, Criteria.where("createdAt").gte(todayStart)));
        today.put("agreements", countOwned("agreements", username, admin, Criteria.where("createdAt").gte(todayStart)));
        today.put("paymentsCollected", sumVerifiedPayments(username, admin, todayStart));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("role", role);
        response.put("username", username);
        response.put("summary", summary);
        response.put("today", today);
        response.put("recentCalls", buildRecentCalls(callScope));
        response.put("performanceTrend", buildPerformanceTrend(weekCalls, weekLeads));
        response.put("leadStatus", buildLeadStatus(statusCounts));
        response.put("telecallerPerformance", admin ? buildTelecallerPerformance(todayStart) : new ArrayList<>());
        return response;
    }

    // ───────────────────────────────────────────────────────── scopes
    /** Telecaller -> leads of assigned districts. Others -> leads assigned to the username. Admin -> null. */
    private Criteria leadScope(String username, String role, boolean admin) {
        if (admin) return null;
        if (username == null || username.isBlank()) return Criteria.where("_id").is("__none__");

        if (role != null && role.toUpperCase(Locale.ROOT).contains("TELECALLER")) {
            Telecaller tc = telecallerRepository.findByUsername(username).orElse(null);
            if (tc != null && tc.getDistricts() != null) {
                List<Object> matchers = new ArrayList<>();
                for (String d : tc.getDistricts()) {
                    if (d != null && !d.trim().isEmpty()) {
                        matchers.add(Pattern.compile("^" + Pattern.quote(d.trim()) + "$", Pattern.CASE_INSENSITIVE));
                    }
                }
                if (!matchers.isEmpty()) return Criteria.where("district").in(matchers);
            }
        }
        return Criteria.where("assignedTo").regex("^" + Pattern.quote(username) + "$", "i");
    }

    private Criteria followUpCriteria() {
        return new Criteria().orOperator(
                Criteria.where("status").regex("follow|call back", "i"),
                Criteria.where("tcNextRefollowupDate").nin(Arrays.asList(null, "")));
    }

    private Criteria connectedCriteria() {
        return new Criteria().orOperator(
                Criteria.where("callStatus").in("connected", "ended", "completed", "COMPLETED", "CONNECTED"),
                Criteria.where("outcome").regex("connected|answered", "i"));
    }

    private Criteria missedCriteria() {
        return new Criteria().orOperator(
                Criteria.where("callStatus").regex("no[ _-]?answer|missed|busy|reject|fail", "i"),
                Criteria.where("outcome").regex("no answer|not answered|missed", "i"));
    }

    // ───────────────────────────────────────────────────────── mongo helpers
    private Query query(Criteria scope, Criteria extra) {
        if (scope != null && extra != null) return new Query(new Criteria().andOperator(scope, extra));
        if (scope != null) return new Query(scope);
        if (extra != null) return new Query(extra);
        return new Query();
    }

    private long countLeads(Criteria scope, Criteria extra) {
        return mongo.count(query(scope, extra), Lead.class);
    }

    private long countCalls(Criteria scope, Criteria extra) {
        return mongo.count(query(scope, extra), Call.class);
    }

    private Map<String, Long> groupCount(String collection, Criteria scope, String field) {
        List<AggregationOperation> ops = new ArrayList<>();
        if (scope != null) ops.add(Aggregation.match(scope));
        ops.add(Aggregation.group(field).count().as("count"));
        AggregationResults<Document> res = mongo.aggregate(Aggregation.newAggregation(ops), collection, Document.class);
        Map<String, Long> out = new LinkedHashMap<>();
        for (Document d : res.getMappedResults()) {
            Object id = d.get("_id");
            Object cnt = d.get("count");
            out.put(id == null ? null : String.valueOf(id), cnt instanceof Number n ? n.longValue() : 0L);
        }
        return out;
    }

    private List<Call> fetchCallsSince(Criteria scope, Date since) {
        Criteria dateCrit = new Criteria().orOperator(
                Criteria.where("startedAt").gte(since),
                Criteria.where("createdAt").gte(since));
        Query q = query(scope, dateCrit);
        q.fields().include("startedAt").include("createdAt").include("callStatus").include("outcome");
        return mongo.find(q, Call.class);
    }

    private List<Lead> fetchLeadDatesSince(Criteria scope, Date since) {
        Query q = query(scope, Criteria.where("createdAt").gte(since));
        q.fields().include("createdAt");
        return mongo.find(q, Lead.class);
    }

    // ───────────────────────────────────────────────────────── sections
    private List<Map<String, Object>> buildRecentCalls(Criteria callScope) {
        Query q = query(callScope, null);
        q.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        q.limit(10);
        q.fields().include("leadName").include("leadMobile").include("outcome")
                .include("callStatus").include("createdAt").include("startedAt")
                .include("agentUsername").include("agentName");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Call c : mongo.find(q, Call.class)) {
            Date d = callDate(c);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("leadName", c.getLeadName());
            item.put("phoneNumber", c.getLeadMobile());
            item.put("outcome", c.getOutcome() != null ? c.getOutcome() : c.getCallStatus());
            item.put("status", c.getCallStatus() != null ? c.getCallStatus() : c.getOutcome());
            item.put("agentUsername", c.getAgentUsername());
            item.put("agentName", c.getAgentName());
            item.put("date", d == null ? null : d.toInstant().atZone(ZONE).toLocalDateTime().toString());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildPerformanceTrend(List<Call> calls, List<Lead> leads) {
        Map<LocalDate, Long> callCounts = new LinkedHashMap<>();
        Map<LocalDate, Long> leadCounts = new LinkedHashMap<>();
        Map<LocalDate, Long> connectedCounts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZONE);
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            callCounts.put(date, 0L);
            leadCounts.put(date, 0L);
            connectedCounts.put(date, 0L);
        }
        for (Call c : calls) {
            Date dv = callDate(c);
            if (dv == null) continue;
            LocalDate d = dv.toInstant().atZone(ZONE).toLocalDate();
            if (callCounts.containsKey(d)) {
                callCounts.put(d, callCounts.get(d) + 1);
                if (isConnected(c)) connectedCounts.put(d, connectedCounts.get(d) + 1);
            }
        }
        for (Lead l : leads) {
            Date dv = l.getCreatedAt();
            if (dv == null) continue;
            LocalDate d = dv.toInstant().atZone(ZONE).toLocalDate();
            if (leadCounts.containsKey(d)) leadCounts.put(d, leadCounts.get(d) + 1);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate date : callCounts.keySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.format(DATE_FORMAT));
            item.put("calls", callCounts.get(date));
            item.put("connected", connectedCounts.get(date));
            item.put("leads", leadCounts.get(date));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildLeadStatus(Map<String, Long> statusCounts) {
        Map<String, Long> merged = new LinkedHashMap<>();
        statusCounts.forEach((status, count) -> {
            String key = (status == null || status.isBlank()) ? "New" : status;
            merged.merge(key, count, Long::sum);
        });
        List<Map<String, Object>> result = new ArrayList<>();
        merged.forEach((status, count) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("count", count);
            result.add(item);
        });
        return result;
    }

    /** Admin only: per-telecaller numbers, using the SAME rules as the summary cards. */
    private List<Map<String, Object>> buildTelecallerPerformance(Date todayStart) {
        // [0]=leads [1]=converted [2]=calls [3]=connected [4]=today calls
        Map<String, long[]> perUser = new LinkedHashMap<>();
        Map<String, Double> paid = new LinkedHashMap<>();

        AggregationResults<Document> leadRes = mongo.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("assignedTo").nin(Arrays.asList(null, ""))),
                        Aggregation.group("assignedTo", "status").count().as("count")),
                "leads", Document.class);
        for (Document d : leadRes.getMappedResults()) {
            Document id = (Document) d.get("_id");
            if (id == null || id.get("assignedTo") == null) continue;
            String u = key(String.valueOf(id.get("assignedTo")));
            String st = id.get("status") == null ? "" : String.valueOf(id.get("status")).toLowerCase(Locale.ROOT);
            long c = ((Number) d.get("count")).longValue();
            long[] row = perUser.computeIfAbsent(u, k -> new long[5]);
            row[0] += c;
            if (st.contains("converted") || st.contains("client") || st.contains("closed won")) row[1] += c;
        }

        // calls: same connected rule as the summary (callStatus OR outcome)
        AggregationResults<Document> callRes = mongo.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("agentUsername").nin(Arrays.asList(null, ""))),
                        Aggregation.group("agentUsername", "callStatus", "outcome").count().as("count")),
                "calls", Document.class);
        for (Document d : callRes.getMappedResults()) {
            Document id = (Document) d.get("_id");
            if (id == null || id.get("agentUsername") == null) continue;
            String u = key(String.valueOf(id.get("agentUsername")));
            String s = id.get("callStatus") == null ? "" : String.valueOf(id.get("callStatus")).toLowerCase(Locale.ROOT);
            String o = id.get("outcome") == null ? "" : String.valueOf(id.get("outcome")).toLowerCase(Locale.ROOT);
            long c = ((Number) d.get("count")).longValue();
            long[] row = perUser.computeIfAbsent(u, k -> new long[5]);
            row[2] += c;
            if (s.equals("connected") || s.equals("ended") || s.equals("completed")
                    || o.contains("connected") || o.contains("answered")) row[3] += c;
        }

        // today's calls per telecaller
        Criteria todayCrit = new Criteria().andOperator(
                Criteria.where("agentUsername").nin(Arrays.asList(null, "")),
                new Criteria().orOperator(Criteria.where("createdAt").gte(todayStart), Criteria.where("startedAt").gte(todayStart)));
        AggregationResults<Document> todayRes = mongo.aggregate(
                Aggregation.newAggregation(Aggregation.match(todayCrit), Aggregation.group("agentUsername").count().as("count")),
                "calls", Document.class);
        for (Document d : todayRes.getMappedResults()) {
            if (d.get("_id") == null) continue;
            perUser.computeIfAbsent(key(String.valueOf(d.get("_id"))), k -> new long[5])[4] += ((Number) d.get("count")).longValue();
        }

        // collected (verified) payments per telecaller
        AggregationResults<Document> payRes = mongo.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(new Criteria().andOperator(verifiedPaymentCriteria(),
                                Criteria.where("createdBy").nin(Arrays.asList(null, "")))),
                        Aggregation.group("createdBy").sum("amountPaid").as("total")),
                "payments", Document.class);
        for (Document d : payRes.getMappedResults()) {
            if (d.get("_id") == null) continue;
            Object t = d.get("total");
            paid.put(key(String.valueOf(d.get("_id"))), t instanceof Number n ? n.doubleValue() : 0.0);
        }

        // every telecaller is listed (even with no activity yet) with the real full name
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (Telecaller tc : telecallerRepository.findAll()) {
            if (tc.getUsername() == null) continue;
            String u = key(tc.getUsername());
            seen.put(u, true);
            result.add(perfRow(tc.getId(), tc.getUsername(), tc.getFullName(), perUser.get(u), paid.get(u)));
        }
        perUser.forEach((u, row) -> {
            if (!seen.containsKey(u)) result.add(perfRow(u, u, u, row, paid.get(u)));
        });
        return result;
    }

    private Map<String, Object> perfRow(String id, String username, String fullName, long[] row, Double paid) {
        long[] r = row == null ? new long[5] : row;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("username", username);
        item.put("fullName", fullName == null || fullName.isBlank() ? username : fullName);
        item.put("totalLeads", r[0]);
        item.put("assignedLeads", r[0]);
        item.put("callsMade", r[2]);
        item.put("todayCalls", r[4]);
        item.put("connectedCalls", r[3]);
        item.put("clientsConverted", r[1]);
        item.put("converted", r[1]);
        item.put("conversionRate", r[0] == 0 ? 0 : round(r[1] * 100.0 / r[0]));
        item.put("paymentsCollected", paid == null ? 0.0 : paid);
        return item;
    }

    private static String key(String username) { return username == null ? "" : username.trim().toLowerCase(Locale.ROOT); }

    // ───────────────────────────────────────────────────────── payments / quotations / agreements
    /** A payment counts as "collected" once the Super Admin has verified it (or it is further along the workflow). */
    private Criteria verifiedPaymentCriteria() {
        return new Criteria().orOperator(
                Criteria.where("verificationStatus").regex("^verified$", "i"),
                Criteria.where("status").regex("^(verified|invoice generated|invoice sent to telecaller|completed|fully paid)$", "i"));
    }

    private double sumVerifiedPayments(String username, boolean admin, Date since) {
        List<Criteria> all = new ArrayList<>();
        all.add(verifiedPaymentCriteria());
        if (!admin) all.add(Criteria.where("createdBy").is(username == null ? "" : username));
        if (since != null) {
            all.add(new Criteria().orOperator(
                    Criteria.where("verifiedAt").gte(since),
                    new Criteria().andOperator(Criteria.where("verifiedAt").exists(false), Criteria.where("updatedAt").gte(since))));
        }
        AggregationResults<Document> res = mongo.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(new Criteria().andOperator(all.toArray(new Criteria[0]))),
                        Aggregation.group().sum("amountPaid").as("total")),
                "payments", Document.class);
        for (Document d : res.getMappedResults()) {
            Object t = d.get("total");
            if (t instanceof Number n) return n.doubleValue();
        }
        return 0.0;
    }

    /** Count documents of a module owned by the user (admins see everything). */
    private long countOwned(String collection, String username, boolean admin, Criteria extra) {
        List<Criteria> all = new ArrayList<>();
        if (extra != null) all.add(extra);
        if (!admin) all.add(Criteria.where("createdBy").is(username == null ? "" : username));
        Query q = all.isEmpty() ? new Query() : new Query(new Criteria().andOperator(all.toArray(new Criteria[0])));
        return mongo.count(q, collection);
    }

    // ───────────────────────────────────────────────────────── small helpers
    private boolean isConnected(Call c) {
        String s = c.getCallStatus() == null ? "" : c.getCallStatus().toLowerCase(Locale.ROOT);
        String o = c.getOutcome() == null ? "" : c.getOutcome().toLowerCase(Locale.ROOT);
        return s.equals("connected") || s.equals("ended") || s.equals("completed")
                || o.contains("connected") || o.contains("answered");
    }

    private boolean isMissed(Call c) {
        String s = c.getCallStatus() == null ? "" : c.getCallStatus().toLowerCase(Locale.ROOT);
        String o = c.getOutcome() == null ? "" : c.getOutcome().toLowerCase(Locale.ROOT);
        return s.matches(".*(no[ _-]?answer|missed|busy|reject|fail).*")
                || o.contains("no answer") || o.contains("not answered") || o.contains("missed");
    }

    private Date callDate(Call c) {
        if (c.getCreatedAt() != null) return c.getCreatedAt();
        return c.getStartedAt();
    }

    private boolean isToday(Date date) {
        if (date == null) return false;
        return date.toInstant().atZone(ZONE).toLocalDate().equals(LocalDate.now(ZONE));
    }

    private String getCurrentUsername() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            return auth == null ? null : auth.getName();
        } catch (Exception e) { return null; }
    }

    private String getCurrentRole() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return "";
            return auth.getAuthorities().stream().map(a -> a.getAuthority()).findFirst().orElse("");
        } catch (Exception e) { return ""; }
    }

    private boolean isAdmin(String role) {
        if (role == null) return false;
        return role.toUpperCase(Locale.ROOT).contains("ADMIN");
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}