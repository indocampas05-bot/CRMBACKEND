package com.indocrm.indocrm.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.indocrm.indocrm.dto.LeadResponse;
import com.indocrm.indocrm.model.Call;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.LeadRepository;
import com.indocrm.indocrm.service.TelecallerAccessService;

/**
 * PERFORMANCE FIX
 * ---------------
 * Old version called leadRepository.findAll() on EVERY request, loaded the whole
 * leads collection (including the heavy excelData + activityHistory fields),
 * filtered in Java and only then paged. That is what made the Call Panel slow
 * (and sometimes never finish through ngrok).
 *
 * New version:
 *   1. filters by the telecaller's districts INSIDE MongoDB (uses the district index)
 *   2. pages inside MongoDB (skip/limit)
 *   3. does NOT load excelData / activityHistory / callHistory for list calls
 *      (activity history is fetched per lead via /api/leads/{id}/history)
 */
@RestController
@RequestMapping("/api/telecaller")
@PreAuthorize("hasRole('TELECALLER')")
public class TelecallerLeadController {

    private final TelecallerAccessService accessService;
    private final LeadRepository leadRepository;
    private final MongoTemplate mongoTemplate;

    public TelecallerLeadController(TelecallerAccessService accessService,
                                    LeadRepository leadRepository,
                                    MongoTemplate mongoTemplate) {
        this.accessService = accessService;
        this.leadRepository = leadRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // ------------------------------------------------------------------ profile
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile() {
        Telecaller tc = accessService.currentTelecaller();
        Map<String, Object> result = new HashMap<>();
        result.put("id", tc.getId());
        result.put("employeeId", tc.getEmployeeId());
        result.put("username", tc.getUsername());
        result.put("fullName", tc.getFullName());
        result.put("role", tc.getRole());
        result.put("districts", accessService.assignedDistricts());
        return ResponseEntity.ok(result);
    }

    // ---------------------------------------------------------------- districts
    @GetMapping("/leads/districts")
    public ResponseEntity<List<Map<String, Object>>> districts() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String district : accessService.assignedDistricts()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", district);
            item.put("district", district);
            // case-insensitive count so it matches what the lead list returns
            item.put("leadCount", mongoTemplate.count(
                    new Query(Criteria.where("district").regex(exactIgnoreCase(district))), Lead.class));
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/leads/district/{district}")
    public ResponseEntity<List<LeadResponse>> districtLeads(@PathVariable String district) {
        accessService.requireDistrictAccess(district);
        Query q = new Query(Criteria.where("district").regex(exactIgnoreCase(district)));
        q.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        applyLightProjection(q);
        List<LeadResponse> result = new ArrayList<>();
        for (Lead l : mongoTemplate.find(q, Lead.class)) result.add(toResponse(l));
        attachCallStats(result);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------- single lead
    @GetMapping("/leads/{leadId}")
    public ResponseEntity<LeadResponse> lead(@PathVariable String leadId) {
        // single lead -> full document (with history) is fine
        return ResponseEntity.ok(toResponse(accessService.requireLeadAccess(leadId)));
    }

    // ------------------------------------------------------- main call-panel list
    @GetMapping("/leads")
    public ResponseEntity<Page<LeadResponse>> leads(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String districts,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<String> assigned = accessService.assignedDistricts();
        if (assigned.isEmpty()) return ResponseEntity.ok(Page.empty(pageable));

        // The telecaller may pick a subset of the districts assigned to them
        // (?districts=Chennai,Madurai). Anything NOT assigned is silently dropped,
        // so a telecaller can never read leads of a district they don't own.
        List<String> selected = assigned;
        if (notBlank(districts)) {
            Set<String> wanted = new LinkedHashSet<>();
            for (String part : districts.split(",")) if (!part.isBlank()) wanted.add(part.trim().toLowerCase());
            selected = assigned.stream()
                    .filter(a -> wanted.contains(a.trim().toLowerCase()))
                    .collect(Collectors.toList());
            if (selected.isEmpty()) return ResponseEntity.ok(Page.empty(pageable));
        }

        // 1) district filter (case-insensitive, still index friendly because it is anchored)
        List<Object> districtMatchers = new ArrayList<>();
        for (String d : selected) districtMatchers.add(exactIgnoreCase(d));

        List<Criteria> and = new ArrayList<>();
        and.add(Criteria.where("district").in(districtMatchers));

        // 2) optional filters
        if (notBlank(search)) {
            Pattern p = Pattern.compile(Pattern.quote(search.trim()), Pattern.CASE_INSENSITIVE);
            and.add(new Criteria().orOperator(
                    Criteria.where("leadId").regex(p),
                    Criteria.where("name").regex(p),
                    Criteria.where("mobile").regex(p),
                    Criteria.where("phoneNumber").regex(p),
                    Criteria.where("school").regex(p),
                    Criteria.where("email").regex(p),
                    Criteria.where("district").regex(p)));
        }
        if (notBlank(category) && !"All Categories".equalsIgnoreCase(category)) {
            and.add(Criteria.where("category").regex(exactIgnoreCase(category.trim())));
        }
        if (notBlank(status) && !"All Status".equalsIgnoreCase(status)) {
            and.add(Criteria.where("status").regex(exactIgnoreCase(status.trim())));
        }

        Query countQuery = new Query(new Criteria().andOperator(and.toArray(new Criteria[0])));
        long total = mongoTemplate.count(countQuery, Lead.class);

        Query dataQuery = new Query(new Criteria().andOperator(and.toArray(new Criteria[0])));
        // never trust an arbitrary sort from the client; default = newest first
        dataQuery.with(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "_id")));
        dataQuery.skip(pageable.getOffset());
        dataQuery.limit(pageable.getPageSize());
        applyLightProjection(dataQuery);

        List<LeadResponse> content = new ArrayList<>();
        for (Lead l : mongoTemplate.find(dataQuery, Lead.class)) content.add(toResponse(l));
        attachCallStats(content);

        return ResponseEntity.ok(new PageImpl<>(content, pageable, total));
    }

    /**
     * One grouped query on the `calls` collection for the leads of the current page:
     * how many times each lead was called (by anyone) and when it was last called.
     * `calls.leadId` stores the business lead id (e.g. LD-1001) = LeadResponse.leadId.
     * Add an index once:  db.calls.createIndex({ leadId: 1, startedAt: -1 })
     */
    private void attachCallStats(List<LeadResponse> leads) {
        if (leads == null || leads.isEmpty()) return;
        List<String> ids = leads.stream().map(LeadResponse::getLeadId)
                .filter(x -> x != null && !x.isBlank()).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("leadId").in(ids)),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "startedAt")),
                Aggregation.group("leadId")
                        .count().as("callCount")
                        .first("startedAt").as("lastCalledAt")
                        .first("agentName").as("lastCalledBy"));
        AggregationResults<Document> res = mongoTemplate.aggregate(agg, Call.class, Document.class);

        Map<String, Document> byLead = new HashMap<>();
        for (Document d : res.getMappedResults()) byLead.put(d.getString("_id"), d);

        for (LeadResponse r : leads) {
            Document d = byLead.get(r.getLeadId());
            if (d == null) { r.setCallCount(0); continue; }
            Number n = d.get("callCount", Number.class);
            r.setCallCount(n == null ? 0 : n.intValue());
            r.setLastCalledAt(d.get("lastCalledAt", Date.class));
            r.setLastCalledBy(d.getString("lastCalledBy"));
        }
    }

    // ------------------------------------------------------------------ helpers
    /** Leave out the heavy fields. They are only needed on the single-lead screens. */
    private void applyLightProjection(Query q) {
        q.fields().exclude("excelData").exclude("activityHistory").exclude("callHistory");
    }

    private Pattern exactIgnoreCase(String value) {
        return Pattern.compile("^" + Pattern.quote(value.trim()) + "$", Pattern.CASE_INSENSITIVE);
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private LeadResponse toResponse(Lead l) {
        LeadResponse r = new LeadResponse();
        r.setId(l.getId()); r.setLeadId(l.getLeadId()); r.setName(l.getName()); r.setMobile(l.getMobile());
        r.setPhoneNumber(l.getPhoneNumber()); r.setEmail(l.getEmail()); r.setCategory(l.getCategory()); r.setCampaign(l.getCampaign());
        r.setStatus(l.getStatus()); r.setAssignedTo(l.getAssignedTo()); r.setSource(l.getSource()); r.setLastContact(l.getLastContact());
        r.setNotes(l.getNotes()); r.setDistrict(l.getDistrict()); r.setArea(l.getArea()); r.setUploadBatch(l.getUploadBatch());
        r.setAssignedDate(l.getAssignedDate()); r.setSlNo(l.getSlNo()); r.setSchool(l.getSchool()); r.setSchoolAddress(l.getSchoolAddress());
        r.setSchoolCurrentLocation(l.getSchoolCurrentLocation()); r.setNoNumber(l.getNoNumber()); r.setNotExistNumber(l.getNotExistNumber());
        r.setNotAnswering(l.getNotAnswering()); r.setTotalNumberOfAnswering(l.getTotalNumberOfAnswering()); r.setAfternoonCallFollowup(l.getAfternoonCallFollowup());
        r.setConveyForHeadOffice(l.getConveyForHeadOffice()); r.setAlreadySoftwareUsers(l.getAlreadySoftwareUsers()); r.setShareProfile(l.getShareProfile());
        r.setTcNextRefollowupDate(l.getTcNextRefollowupDate()); r.setMeNextVisitDate(l.getMeNextVisitDate()); r.setFiftyPercentSubscription(l.getFiftyPercentSubscription());
        r.setNewSchoolFound(l.getNewSchoolFound()); r.setDemoScheduling(l.getDemoScheduling()); r.setAcceptedSubscription(l.getAcceptedSubscription());
        r.setRejected(l.getRejected()); r.setSchoolClosed(l.getSchoolClosed()); r.setNumberOfNoNeed(l.getNumberOfNoNeed());
        r.setCmoVisitWithMarketingExecutive(l.getCmoVisitWithMarketingExecutive()); r.setNextAcademyYearFollowup(l.getNextAcademyYearFollowup());
        r.setIcInstalled(l.getIcInstalled()); r.setAppointmentSecured(l.getAppointmentSecured()); r.setDirectVisit(l.getDirectVisit());
        r.setUp1(l.getUp1()); r.setUp2(l.getUp2()); r.setUp3(l.getUp3()); r.setUp4(l.getUp4()); r.setUp5(l.getUp5());
        r.setCreatedAt(l.getCreatedAt()); r.setUpdatedAt(l.getUpdatedAt()); r.setUpdates(l.getUpdates());
        r.setExcelData(l.getExcelData());
        r.setActivityHistory(l.getActivityHistory());
        return r;
    }
}