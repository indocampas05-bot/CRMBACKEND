package com.indocrm.indocrm.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.model.Call;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.CallRepository;
import com.indocrm.indocrm.repository.LeadRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;

@Service
public class ReportService {

    private final LeadRepository leadRepository;
    private final CallRepository callRepository;
    private final TelecallerRepository telecallerRepository;

    public ReportService(
            LeadRepository leadRepository,
            CallRepository callRepository,
            TelecallerRepository telecallerRepository) {
        this.leadRepository = leadRepository;
        this.callRepository = callRepository;
        this.telecallerRepository = telecallerRepository;
    }

    public Map<String, Object> myReport(String period) {
        Authentication auth = authentication();
        requireRole("ROLE_TELECALLER");
        return buildReport(auth.getName(), period);
    }

    public Map<String, Object> overallReport(String period) {
        requireAdmin();
        return buildReport(null, period);
    }

    @SuppressWarnings("unchecked")

    public List<Map<String, Object>> telecallerReports(String period) {
        requireAdmin();

        return telecallerRepository.findAll().stream()
                .filter(tc -> tc.isEnabled())
                .filter(tc -> tc.getUsername() != null && !tc.getUsername().isBlank())
                .map(tc -> {
                    Map<String, Object> report = buildReport(tc.getUsername(), period);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", tc.getId());
                    row.put("employeeId", tc.getEmployeeId());
                    row.put("username", tc.getUsername());
                    row.put("fullName", tc.getFullName());
                    row.put("status", tc.getStatus());
                    row.put("target", tc.getTarget());
                    row.put("report", report);
                    return row;
                })
               .sorted((a, b) -> {
    Map<String, Object> bReport =
            (Map<String, Object>) b.get("report");

    Map<String, Object> aReport =
            (Map<String, Object>) a.get("report");

    Map<String, Object> bSummary =
            (Map<String, Object>) bReport.get("summary");

    Map<String, Object> aSummary =
            (Map<String, Object>) aReport.get("summary");

    return Long.compare(
            number(bSummary.get("callsMade")),
            number(aSummary.get("callsMade"))
    );
})
                .collect(Collectors.toList());
    }

    public Map<String, Object> telecallerReport(String username, String period) {
        requireAdmin();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Telecaller username is required");
        }

        if (telecallerRepository.findByUsername(username).isEmpty()) {
            throw new IllegalArgumentException("Telecaller not found");
        }

        return buildReport(username, period);
    }

    private Map<String, Object> buildReport(String username, String period) {
        Range range = resolveRange(period);

        List<Lead> allLeads = leadRepository.findAll();
        List<Call> allCalls = callRepository.findAll();

        List<Lead> leads = allLeads.stream()
                .filter(lead -> username == null || username.equalsIgnoreCase(safe(lead.getAssignedTo())))
                .filter(lead -> inRange(lead.getCreatedAt(), range))
                .collect(Collectors.toList());

        List<Lead> allAssignedLeads = allLeads.stream()
                .filter(lead -> username == null || username.equalsIgnoreCase(safe(lead.getAssignedTo())))
                .collect(Collectors.toList());

        List<Call> calls = allCalls.stream()
                .filter(call -> username == null || username.equalsIgnoreCase(safe(call.getAgentUsername())))
                .filter(call -> inRange(callTime(call), range))
                .collect(Collectors.toList());

        List<Call> todayCalls = allCalls.stream()
                .filter(call -> username == null || username.equalsIgnoreCase(safe(call.getAgentUsername())))
                .filter(call -> inRange(callTime(call), todayRange()))
                .collect(Collectors.toList());

        long followUps = allAssignedLeads.stream()
                .filter(lead -> "FOLLOW-UP".equalsIgnoreCase(safe(lead.getStatus()))
                        || "FOLLOW UP".equalsIgnoreCase(safe(lead.getStatus())))
                .count();

        long converted = allAssignedLeads.stream()
                .filter(lead -> "CONVERTED".equalsIgnoreCase(safe(lead.getStatus())))
                .count();

        long interested = allAssignedLeads.stream()
                .filter(lead -> "INTERESTED".equalsIgnoreCase(safe(lead.getStatus())))
                .count();

        long connected = calls.stream().filter(this::isConnected).count();
        long missed = Math.max(0, calls.size() - connected);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalLeads", leads.size());
        summary.put("assignedLeads", allAssignedLeads.size());
        summary.put("callsMade", calls.size());
        summary.put("connectedCalls", connected);
        summary.put("missedCalls", missed);
        summary.put("followUps", followUps);
        summary.put("interestedLeads", interested);
        summary.put("clientsConverted", converted);
        summary.put("conversionRate", percentage(converted, allAssignedLeads.size()));
        summary.put("connectionRate", percentage(connected, calls.size()));

        Map<String, Object> today = new LinkedHashMap<>();
        today.put("callsMade", todayCalls.size());
        today.put("connectedCalls", todayCalls.stream().filter(this::isConnected).count());
        today.put("missedCalls", Math.max(0, todayCalls.size() - todayCalls.stream().filter(this::isConnected).count()));
        today.put("newLeadsContacted", todayCalls.stream()
                .map(Call::getLeadId).filter(Objects::nonNull).distinct().count());
        today.put("followUpsCompleted", todayCalls.stream()
                .filter(c -> containsIgnoreCase(c.getOutcome(), "follow"))
                .count());
        today.put("interestedLeads", todayCalls.stream()
                .filter(c -> containsIgnoreCase(c.getOutcome(), "interest"))
                .count());

        Map<String, Long> statusCounts = allAssignedLeads.stream()
                .collect(Collectors.groupingBy(
                        lead -> {
                            String status = safe(lead.getStatus());
                            return status.isBlank() ? "Unspecified" : status;
                        },
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        List<Map<String, Object>> leadStatus = statusCounts.entrySet().stream()
                .map(e -> mapOf("status", e.getKey(), "count", e.getValue()))
                .sorted((a, b) -> Long.compare(number(b.get("count")), number(a.get("count"))))
                .collect(Collectors.toList());

        Map<String, Long> callOutcomeCounts = calls.stream()
                .collect(Collectors.groupingBy(
                        call -> {
                            String outcome = safe(call.getOutcome());
                            if (outcome.isBlank()) outcome = safe(call.getCallStatus());
                            return outcome.isBlank() ? "Unknown" : outcome;
                        },
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        List<Map<String, Object>> callOutcomes = callOutcomeCounts.entrySet().stream()
                .map(e -> mapOf("outcome", e.getKey(), "count", e.getValue()))
                .sorted((a, b) -> Long.compare(number(b.get("count")), number(a.get("count"))))
                .collect(Collectors.toList());

        Map<String, Long> callsByDay = calls.stream()
                .collect(Collectors.groupingBy(
                        call -> dayKey(callTime(call)),
                        TreeMap::new,
                        Collectors.counting()
                ));

        List<Map<String, Object>> performanceTrend = callsByDay.entrySet().stream()
                .map(e -> mapOf("date", e.getKey(), "calls", e.getValue()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", username == null ? "ALL_TELECALLERS" : "TELECALLER");
        result.put("username", username);
        result.put("period", normalizePeriod(period));
        result.put("generatedAt", Instant.now().toString());
        result.put("summary", summary);
        result.put("today", today);
        result.put("leadStatus", leadStatus);
        result.put("callOutcomes", callOutcomes);
        result.put("performanceTrend", performanceTrend);
        return result;
    }

    private Authentication authentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return auth;
    }

    private void requireRole(String role) {
        boolean allowed = authentication().getAuthorities().stream()
                .anyMatch(a -> role.equals(a.getAuthority()));
        if (!allowed) throw new AccessDeniedException("You are not allowed to access this report");
    }

    private void requireAdmin() {
        boolean allowed = authentication().getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(role -> "ROLE_SUPER_ADMIN".equals(role) || "ROLE_ADMIN".equals(role));
        if (!allowed) throw new AccessDeniedException("Only Super Admin/Admin can access this report");
    }

    private Range resolveRange(String rawPeriod) {
        String period = normalizePeriod(rawPeriod);
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime start;

        switch (period) {
            case "TODAY":
                start = now.toLocalDate().atStartOfDay(zone);
                break;
            case "WEEK":
                start = now.minusDays(6).toLocalDate().atStartOfDay(zone);
                break;
            case "MONTH":
                start = now.minusDays(29).toLocalDate().atStartOfDay(zone);
                break;
            case "ALL":
            default:
                return new Range(null, null);
        }

        return new Range(Date.from(start.toInstant()), Date.from(now.plusSeconds(1).toInstant()));
    }

    private Range todayRange() {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime start = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone);
        return new Range(Date.from(start.toInstant()), Date.from(start.plusDays(1).toInstant()));
    }

    private String normalizePeriod(String value) {
        if (value == null) return "MONTH";
        String normalized = value.trim().toUpperCase().replace(" ", "_");
        if ("THIS_WEEK".equals(normalized) || "WEEK".equals(normalized)) return "WEEK";
        if ("THIS_MONTH".equals(normalized) || "MONTH".equals(normalized)) return "MONTH";
        if ("TODAY".equals(normalized)) return "TODAY";
        if ("ALL".equals(normalized) || "ALL_TIME".equals(normalized)) return "ALL";
        return "MONTH";
    }

    private boolean inRange(Date date, Range range) {
        if (range.start == null || range.end == null) return true;
        if (date == null) return false;
        return !date.before(range.start) && date.before(range.end);
    }

    private Date callTime(Call call) {
        if (call.getStartedAt() != null) return call.getStartedAt();
        if (call.getCreatedAt() != null) return call.getCreatedAt();
        return call.getUpdatedAt();
    }

    private boolean isConnected(Call call) {
        String status = safe(call.getCallStatus()).toLowerCase();
        String outcome = safe(call.getOutcome()).toLowerCase();
        return status.contains("connected")
                || status.contains("completed")
                || status.contains("answered")
                || outcome.contains("connected")
                || outcome.contains("interested")
                || outcome.contains("follow");
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return safe(value).toLowerCase().contains(expected.toLowerCase());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private double percentage(long value, long total) {
        if (total <= 0) return 0D;
        return Math.round((value * 10000D / total)) / 100D;
    }

    private String dayKey(Date date) {
        if (date == null) return "Unknown";
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    private Map<String, Object> mapOf(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    private static class Range {
        private final Date start;
        private final Date end;

        private Range(Date start, Date end) {
            this.start = start;
            this.end = end;
        }
    }
}
