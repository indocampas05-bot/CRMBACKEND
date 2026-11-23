// E:\indocrm\src\main\java\com\indocrm\indocrm\service\LeadService.java

package com.indocrm.indocrm.service;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.dto.LeadRequest;
import com.indocrm.indocrm.dto.LeadResponse;
import com.indocrm.indocrm.model.CallHistory;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.LeadActivity;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.LeadRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;

@Service
public class LeadService {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TelecallerRepository telecallerRepository;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    // ===================== FOLLOW-UP DATE NORMALISATION =====================
    private static final DateTimeFormatter[] ACCEPTED_DATE_INPUT_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("dd-MM-yyyy").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withResolverStyle(ResolverStyle.STRICT)
    };
    private static final DateTimeFormatter DATE_STORAGE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter EXCEL_DATE_CELL_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ===================== NORMALIZE EXCEL ROW =====================
    /**
     * Normalizes Excel column names by trimming, lowercasing, and removing
     * non-alphanumeric characters. This allows case-insensitive matching of
     * column names like "DISTRICT", "District", "district", etc.
     */
    private Map<String, Object> normalizeExcelRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (row == null) {
            return normalized;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String normalizedKey = entry.getKey()
                    .trim()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
            normalized.put(normalizedKey, entry.getValue());
        }
        return normalized;
    }

    private String normalizeFollowUpDate(String rawValue, int excelRowNumber, String columnName, List<String> errors) {
        if (rawValue == null) return null;
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) return null;

        for (DateTimeFormatter formatter : ACCEPTED_DATE_INPUT_FORMATS) {
            try {
                LocalDate parsed = LocalDate.parse(trimmed, formatter);
                return parsed.format(DATE_STORAGE_FORMAT);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        errors.add("Invalid date format at Row " + excelRowNumber + ", Column " + columnName + " (value: " + trimmed + ")");
        return null;
    }

    // ===================== GET ALL LEADS WITH FILTERS =====================
    public Page<LeadResponse> getAllLeads(String search, String category, String status, String assignedTo, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        boolean telecaller = isTelecaller(auth);

        if (telecaller) {
            Telecaller tc = getCurrentTelecaller(username);
            List<String> districts = tc.getDistricts() == null ? Collections.emptyList() : tc.getDistricts();
            if (districts.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            criteriaList.add(Criteria.where("district").in(districts));
        } else if (assignedTo != null && !assignedTo.trim().isEmpty()) {
            criteriaList.add(Criteria.where("assignedTo").is(assignedTo.trim()));
        }

        if (search != null && !search.trim().isEmpty()) {
            String pattern = ".*" + java.util.regex.Pattern.quote(search.trim()) + ".*";
            criteriaList.add(new Criteria().orOperator(
                Criteria.where("leadId").regex(pattern, "i"),
                Criteria.where("name").regex(pattern, "i"),
                Criteria.where("school").regex(pattern, "i"),
                Criteria.where("mobile").regex(pattern, "i"),
                Criteria.where("phoneNumber").regex(pattern, "i"),
                Criteria.where("email").regex(pattern, "i"),
                Criteria.where("district").regex(pattern, "i"),
                Criteria.where("area").regex(pattern, "i"),
                Criteria.where("schoolAddress").regex(pattern, "i"),
                Criteria.where("notes").regex(pattern, "i")
            ));
        }
        if (category != null && !category.trim().isEmpty() && !category.equals("All Categories")) {
            criteriaList.add(Criteria.where("category").is(category));
        }
        if (status != null && !status.trim().isEmpty() && !status.equals("All Status")) {
            criteriaList.add(Criteria.where("status").is(status));
        }
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        query.with(pageable);
        long total = mongoTemplate.count(query, Lead.class);
        List<Lead> leads = mongoTemplate.find(query, Lead.class);
        return new PageImpl<>(leads.stream().map(this::convertToResponse).collect(Collectors.toList()), pageable, total);
    }

    // ===================== GET ALL LEADS (PAGINATED) =====================
    public Page<LeadResponse> getAllLeads(Pageable pageable) {
        return getAllLeads(null, null, null, null, pageable);
    }

    // ===================== CREATE LEAD =====================
    public LeadResponse createLead(LeadRequest request) {
        String leadId = generateLeadId();
        Lead lead = new Lead();

        lead.setLeadId(leadId);
        lead.setName(request.getName());
        lead.setMobile(request.getMobile());
        lead.setPhoneNumber(request.getMobile());
        lead.setEmail(request.getEmail());
        lead.setCategory(request.getCategory() != null ? request.getCategory() : "School");
        lead.setCampaign(request.getCampaign());
        lead.setStatus(request.getStatus() != null ? request.getStatus() : "New");
        lead.setAssignedTo(request.getAssignedTo() != null ? request.getAssignedTo() : "Unassigned");
        lead.setSource(request.getSource() != null ? request.getSource() : "Excel Import");
        lead.setNotes(request.getNotes());
        lead.setDistrict(request.getDistrict());
        lead.setArea(request.getArea());
        lead.setUploadBatch(request.getUploadBatch());
        lead.setAssignedDate(request.getAssignedDate());

        if (request.getSlNo() != null) {
            try {
                lead.setSlNo(Integer.parseInt(request.getSlNo()));
            } catch (NumberFormatException ignored) {}
        }
        lead.setSchool(request.getSchool());
        lead.setSchoolAddress(request.getSchoolAddress());
        lead.setSchoolCurrentLocation(request.getSchoolCurrentLocation());

        lead.setNoNumber(request.getNoNumber());
        lead.setNotExistNumber(request.getNotExistNumber());
        lead.setNotAnswering(request.getNotAnswering());
        lead.setTotalNumberOfAnswering(request.getTotalNumberOfAnswering());
        lead.setAfternoonCallFollowup(request.getAfternoonCallFollowup());
        lead.setConveyForHeadOffice(request.getConveyForHeadOffice());

        lead.setAlreadySoftwareUsers(request.getAlreadySoftwareUsers());
        lead.setShareProfile(request.getShareProfile());
        lead.setTcNextRefollowupDate(request.getTcNextRefollowupDate());
        lead.setMeNextVisitDate(request.getMeNextVisitDate());
        lead.setFiftyPercentSubscription(request.getFiftyPercentSubscription());
        lead.setNewSchoolFound(request.getNewSchoolFound());
        lead.setDemoScheduling(request.getDemoScheduling());
        lead.setAcceptedSubscription(request.getAcceptedSubscription());
        lead.setRejected(request.getRejected());
        lead.setSchoolClosed(request.getSchoolClosed());
        lead.setNumberOfNoNeed(request.getNumberOfNoNeed());
        lead.setCmoVisitWithMarketingExecutive(request.getCmoVisitWithMarketingExecutive());
        lead.setNextAcademyYearFollowup(request.getNextAcademyYearFollowup());
        lead.setIcInstalled(request.getIcInstalled());
        lead.setAppointmentSecured(request.getAppointmentSecured());
        lead.setDirectVisit(request.getDirectVisit());

        lead.setUp1(request.getUp1());
        lead.setUp2(request.getUp2());
        lead.setUp3(request.getUp3());
        lead.setUp4(request.getUp4());
        lead.setUp5(request.getUp5());

        lead.setUpdates(request.getUpdates() != null ? request.getUpdates() : new ArrayList<>());

        lead.setLastContact("--");
        lead.setCreatedAt(new Date());
        lead.setUpdatedAt(new Date());
        lead.setCallHistory(new CallHistory());
        lead.setActivityHistory(new ArrayList<>());

        Lead savedLead = leadRepository.save(lead);
        recordActivity(savedLead, "LEAD_CREATED", savedLead.getSource(), "Lead created", null, null, null,
                "New lead created", currentUsernameOr("System"));
        savedLead = leadRepository.save(savedLead);
        return convertToResponse(savedLead);
    }

    // ===================== UPDATE LEAD =====================
    public LeadResponse updateLead(String leadId, LeadRequest request) {
        Lead lead = leadRepository.findByLeadId(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with ID: " + leadId));
        assertLeadAccess(lead);

        Map<String, String> before = captureLeadFields(lead);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = isAdmin(auth);
        String username = currentUsernameOr("System");

        if (request.getName() != null) lead.setName(request.getName());
        if (request.getMobile() != null) { lead.setMobile(request.getMobile()); lead.setPhoneNumber(request.getMobile()); }
        if (request.getEmail() != null) lead.setEmail(request.getEmail());
        if (request.getCategory() != null) lead.setCategory(request.getCategory());
        if (request.getCampaign() != null) lead.setCampaign(request.getCampaign());
        if (request.getStatus() != null) lead.setStatus(request.getStatus());
        if (admin && request.getAssignedTo() != null) lead.setAssignedTo(request.getAssignedTo());
        if (request.getSource() != null) lead.setSource(request.getSource());
        if (request.getNotes() != null) {
            lead.setNotes(request.getNotes());
            // Call Panel saves UP1, UP2 ... UPn inside the notes JSON - mirror them into the
            // Excel snapshot so the Leads / district table (Super Admin + Telecaller) shows them.
            syncUpdateEntriesToExcelData(lead, request.getNotes());
        }
        if (admin && request.getDistrict() != null) lead.setDistrict(request.getDistrict());
        if (request.getArea() != null) lead.setArea(request.getArea());
        if (request.getUploadBatch() != null) lead.setUploadBatch(request.getUploadBatch());
        if (admin && request.getAssignedDate() != null) lead.setAssignedDate(request.getAssignedDate());
        if (request.getSlNo() != null && !request.getSlNo().trim().isEmpty()) {
            try { lead.setSlNo(Integer.parseInt(request.getSlNo())); } catch (NumberFormatException ignored) {}
        }
        if (request.getSchool() != null) lead.setSchool(request.getSchool());
        if (request.getSchoolAddress() != null) lead.setSchoolAddress(request.getSchoolAddress());
        if (request.getSchoolCurrentLocation() != null) lead.setSchoolCurrentLocation(request.getSchoolCurrentLocation());
        if (request.getNoNumber() != null) lead.setNoNumber(request.getNoNumber());
        if (request.getNotExistNumber() != null) lead.setNotExistNumber(request.getNotExistNumber());
        if (request.getNotAnswering() != null) lead.setNotAnswering(request.getNotAnswering());
        if (request.getTotalNumberOfAnswering() != null) lead.setTotalNumberOfAnswering(request.getTotalNumberOfAnswering());
        if (request.getAfternoonCallFollowup() != null) lead.setAfternoonCallFollowup(request.getAfternoonCallFollowup());
        if (request.getConveyForHeadOffice() != null) lead.setConveyForHeadOffice(request.getConveyForHeadOffice());
        if (request.getAlreadySoftwareUsers() != null) lead.setAlreadySoftwareUsers(request.getAlreadySoftwareUsers());
        if (request.getShareProfile() != null) lead.setShareProfile(request.getShareProfile());
        if (request.getTcNextRefollowupDate() != null) lead.setTcNextRefollowupDate(request.getTcNextRefollowupDate());
        if (request.getMeNextVisitDate() != null) lead.setMeNextVisitDate(request.getMeNextVisitDate());
        if (request.getFiftyPercentSubscription() != null) lead.setFiftyPercentSubscription(request.getFiftyPercentSubscription());
        if (request.getNewSchoolFound() != null) lead.setNewSchoolFound(request.getNewSchoolFound());
        if (request.getDemoScheduling() != null) lead.setDemoScheduling(request.getDemoScheduling());
        if (request.getAcceptedSubscription() != null) lead.setAcceptedSubscription(request.getAcceptedSubscription());
        if (request.getRejected() != null) lead.setRejected(request.getRejected());
        if (request.getSchoolClosed() != null) lead.setSchoolClosed(request.getSchoolClosed());
        if (request.getNumberOfNoNeed() != null) lead.setNumberOfNoNeed(request.getNumberOfNoNeed());
        if (request.getCmoVisitWithMarketingExecutive() != null) lead.setCmoVisitWithMarketingExecutive(request.getCmoVisitWithMarketingExecutive());
        if (request.getNextAcademyYearFollowup() != null) lead.setNextAcademyYearFollowup(request.getNextAcademyYearFollowup());
        if (request.getIcInstalled() != null) lead.setIcInstalled(request.getIcInstalled());
        if (request.getAppointmentSecured() != null) lead.setAppointmentSecured(request.getAppointmentSecured());
        if (request.getDirectVisit() != null) lead.setDirectVisit(request.getDirectVisit());
        if (request.getUp1() != null) lead.setUp1(request.getUp1());
        if (request.getUp2() != null) lead.setUp2(request.getUp2());
        if (request.getUp3() != null) lead.setUp3(request.getUp3());
        if (request.getUp4() != null) lead.setUp4(request.getUp4());
        if (request.getUp5() != null) lead.setUp5(request.getUp5());
        if (request.getUpdates() != null) lead.setUpdates(request.getUpdates());

        lead.setUpdatedAt(new Date());
        Lead updatedLead = leadRepository.save(lead);

        String source = request.getSource() != null ? request.getSource() : (admin ? "Super Admin" : "Telecaller");
        recordFieldDiffs(updatedLead, before, source, source.toUpperCase().replace(' ', '_') + "_UPDATE", username);
        updatedLead = leadRepository.save(updatedLead);
        return convertToResponse(updatedLead);
    }

    // ===================== TELECALLER UPDATES (UP1, UP2, ... UPn) =====================
    // Matches "UP1", "up1", "UP 1", "Up_2" ...
    private static final Pattern UP_KEY_PATTERN =
            Pattern.compile("^\\s*up\\s*[-_ ]?\\s*(\\d{1,4})\\s*$", Pattern.CASE_INSENSITIVE);
    private static final com.fasterxml.jackson.databind.ObjectMapper UP_JSON_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private static int upNumber(String key) {
        if (key == null) return -1;
        Matcher m = UP_KEY_PATTERN.matcher(key);
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static boolean hasText(Object value) {
        return value != null && !String.valueOf(value).trim().isEmpty();
    }

    /**
     * Copies every UPn found in the notes JSON into lead.excelData (the complete Excel row that the
     * Leads / district table renders). An existing Excel value is never overwritten - the update is
     * only written when that UP cell is missing or blank. Leads that have no Excel snapshot are left
     * alone (the frontends fall back to the notes JSON for them).
     */
    private void syncUpdateEntriesToExcelData(Lead lead, String notes) {
        if (lead == null || notes == null) return;
        String trimmed = notes.trim();
        if (!trimmed.startsWith("{")) return;

        Map<String, Object> currentExcel = lead.getExcelData();
        if (currentExcel == null || currentExcel.isEmpty()) return;

        Map<String, Object> parsed;
        try {
            parsed = UP_JSON_MAPPER.readValue(trimmed,
                    new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ignored) {
            return; // plain-text note, nothing to sync
        }

        Map<String, Object> excel = new LinkedHashMap<>(currentExcel);
        boolean changed = false;

        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            int n = upNumber(entry.getKey());
            if (n < 0 || !hasText(entry.getValue())) continue;
            String value = String.valueOf(entry.getValue()).trim();

            String targetKey = null;
            for (String k : excel.keySet()) {
                if (upNumber(k) == n) { targetKey = k; break; }
            }
            if (targetKey == null) targetKey = "UP" + n;

            if (!hasText(excel.get(targetKey))) {
                excel.put(targetKey, value);
                changed = true;
            }
            applyLegacyUpField(lead, n, value);
        }

        if (changed) lead.setExcelData(excel);
    }

    /** Keeps the old up1..up5 fields filled too (only when they are still empty). */
    private void applyLegacyUpField(Lead lead, int n, String value) {
        switch (n) {
            case 1: if (!hasText(lead.getUp1())) lead.setUp1(value); break;
            case 2: if (!hasText(lead.getUp2())) lead.setUp2(value); break;
            case 3: if (!hasText(lead.getUp3())) lead.setUp3(value); break;
            case 4: if (!hasText(lead.getUp4())) lead.setUp4(value); break;
            case 5: if (!hasText(lead.getUp5())) lead.setUp5(value); break;
            default: break;
        }
    }

    /**
     * An Excel re-import replaces excelData with the new Excel row. Put back any UPn update that
     * the new row does not contain, so updates typed by telecallers are never lost on re-import.
     */
    private void preserveTelecallerUpdates(Map<String, Object> oldExcel, Lead lead) {
        if (oldExcel == null || oldExcel.isEmpty() || lead == null || lead.getExcelData() == null) return;
        Map<String, Object> merged = new LinkedHashMap<>(lead.getExcelData());
        boolean changed = false;

        for (Map.Entry<String, Object> old : oldExcel.entrySet()) {
            int n = upNumber(old.getKey());
            if (n < 0 || !hasText(old.getValue())) continue;

            boolean alreadyFilled = false;
            List<String> blankSameNumber = new ArrayList<>();
            for (Map.Entry<String, Object> now : merged.entrySet()) {
                if (upNumber(now.getKey()) != n) continue;
                if (hasText(now.getValue())) alreadyFilled = true;
                else blankSameNumber.add(now.getKey());
            }
            if (alreadyFilled) continue;

            for (String blankKey : blankSameNumber) merged.remove(blankKey);
            merged.put(old.getKey(), old.getValue());
            changed = true;
        }

        if (changed) lead.setExcelData(merged);
    }

    // ===================== GET LEAD BY ID =====================
    public LeadResponse getLeadById(String leadId) {
        Lead lead = leadRepository.findByLeadId(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found with ID: " + leadId));
        assertLeadAccess(lead);
        return convertToResponse(lead);
    }

    // ===================== GET LEADS BY DISTRICT =====================
    public List<LeadResponse> getLeadsByDistrict(String district) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTelecaller(auth)) { assertDistrictAccess(district); }
        return leadRepository.findByDistrict(district).stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // ===================== MY DISTRICTS =====================
    public List<Map<String, Object>> getMyDistricts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isTelecaller(auth)) {
            return leadRepository.findAll().stream()
                    .filter(l -> l.getDistrict() != null && !l.getDistrict().trim().isEmpty())
                    .collect(Collectors.groupingBy(Lead::getDistrict, Collectors.counting()))
                    .entrySet().stream().map(e -> { Map<String,Object> m=new HashMap<>(); m.put("district",e.getKey()); m.put("leadCount",e.getValue()); return m; })
                    .sorted((a,b)->String.valueOf(a.get("district")).compareToIgnoreCase(String.valueOf(b.get("district"))))
                    .collect(Collectors.toList());
        }
        Telecaller tc = getCurrentTelecaller(auth.getName());
        List<Map<String,Object>> result = new ArrayList<>();
        for (String district : tc.getDistricts() == null ? Collections.<String>emptyList() : tc.getDistricts()) {
            Map<String,Object> m = new HashMap<>();
            m.put("district", district);
            m.put("leadCount", leadRepository.countByDistrict(district));
            result.add(m);
        }
        result.sort((a,b)->String.valueOf(a.get("district")).compareToIgnoreCase(String.valueOf(b.get("district"))));
        return result;
    }

    // ===================== MY LEADS BY DISTRICT - FIXED =====================
    public List<LeadResponse> getMyLeadsByDistrict(String district) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (district == null || district.trim().isEmpty()) {
            throw new IllegalArgumentException("District is required");
        }

        String requestedDistrict = district.trim();

        // ---------------------------------------------------------
        // SECURITY
        // ---------------------------------------------------------
        // A telecaller can only request an assigned district.
        // Admin/Super Admin can access any district.
        // ---------------------------------------------------------

        if (isTelecaller(auth)) {
            assertDistrictAccess(requestedDistrict);
        }

        // ---------------------------------------------------------
        // CASE-INSENSITIVE DISTRICT SEARCH
        // ---------------------------------------------------------
        // This prevents:
        // "Villupuram", "VILLUPURAM", "villupuram"
        // from becoming different results.
        // ---------------------------------------------------------

        List<Lead> leads = leadRepository.findByDistrictIgnoreCase(requestedDistrict);

        // ---------------------------------------------------------
        // RETURN THE EXACT SAME LEAD OBJECT DATA
        // THAT SUPER ADMIN USES
        // ---------------------------------------------------------

        return leads.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LeadActivity> getLeadHistory(String leadId) {
        Lead lead = leadRepository.findByLeadId(leadId).orElseThrow(() -> new RuntimeException("Lead not found with ID: " + leadId));
        assertLeadAccess(lead);
        List<LeadActivity> history = lead.getActivityHistory() == null ? new ArrayList<>() : new ArrayList<>(lead.getActivityHistory());
        history.sort((a,b)-> { Date da=a.getCreatedAt(), db=b.getCreatedAt(); if(da==null&&db==null)return 0; if(da==null)return 1; if(db==null)return -1; return db.compareTo(da); });
        return history;
    }

    // ===================== GET LEADS BY STATUS =====================
    public List<LeadResponse> getLeadsByStatus(String status) {
        return leadRepository.findByStatus(status)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ===================== GET LEADS BY ASSIGNED TO =====================
    public List<LeadResponse> getLeadsByAssignedTo(String assignedTo) {
        return leadRepository.findByAssignedTo(assignedTo)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ===================== SEARCH LEADS =====================
    public List<LeadResponse> searchLeads(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllLeads(Pageable.unpaged()).getContent();
        }

        Query query = new Query();
        String pattern = ".*" + keyword.trim() + ".*";

        query.addCriteria(new Criteria().orOperator(
                Criteria.where("name").regex(pattern, "i"),
                Criteria.where("school").regex(pattern, "i"),
                Criteria.where("mobile").regex(pattern, "i"),
                Criteria.where("schoolAddress").regex(pattern, "i"),
                Criteria.where("district").regex(pattern, "i"),
                Criteria.where("leadId").regex(pattern, "i"),
                Criteria.where("status").regex(pattern, "i")
        ));

        List<Lead> leads = mongoTemplate.find(query, Lead.class);
        return leads.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // ===================== GET DASHBOARD STATISTICS =====================
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalSchools = leadRepository.count();
        stats.put("totalSchools", totalSchools);

        stats.put("noNumber", leadRepository.countByNoNumber("Yes"));
        stats.put("notExistNumber", leadRepository.countByNotExistNumber("Yes"));
        stats.put("notAnswering", leadRepository.countByNotAnswering("Yes"));
        stats.put("totalNumberOfAnswering", leadRepository.countByTotalNumberOfAnswering("Yes"));
        stats.put("alreadySoftwareUsers", leadRepository.countByAlreadySoftwareUsers("Yes"));
        stats.put("shareProfile", leadRepository.countByShareProfile("Yes"));
        stats.put("demoScheduling", leadRepository.countByDemoScheduling("Yes"));
        stats.put("acceptedSubscription", leadRepository.countByAcceptedSubscription("Yes"));
        stats.put("rejected", leadRepository.countByRejected("Yes"));
        stats.put("schoolClosed", leadRepository.countBySchoolClosed("Yes"));
        stats.put("numberOfNoNeed", leadRepository.countByNumberOfNoNeed("Yes"));
        stats.put("appointmentSecured", leadRepository.countByAppointmentSecured("Yes"));
        stats.put("icInstalled", leadRepository.countByIcInstalled("Yes"));

        stats.put("tcFollowupPending", leadRepository.countByTcNextRefollowupDateIsNotNull());

        List<Lead> allLeads = leadRepository.findAll();
        Map<String, Long> districtWise = allLeads.stream()
                .filter(l -> l.getDistrict() != null && !l.getDistrict().isEmpty())
                .collect(Collectors.groupingBy(Lead::getDistrict, Collectors.counting()));
        stats.put("districtWise", districtWise);

        Pageable pageable = Pageable.ofSize(10);
        List<Lead> recentLeads = leadRepository.findAll(pageable).getContent();
        stats.put("recentLeads", recentLeads.stream().map(this::convertToResponse).collect(Collectors.toList()));

        return stats;
    }

    // ===================== GET DISTRICT WISE COUNTS =====================
    public Map<String, Long> getDistrictWiseCounts() {
        List<Lead> allLeads = leadRepository.findAll();
        return allLeads.stream()
                .filter(l -> l.getDistrict() != null && !l.getDistrict().isEmpty())
                .collect(Collectors.groupingBy(Lead::getDistrict, Collectors.counting()));
    }

    // ===================== GET STATS BY DISTRICT =====================
    public Map<String, Object> getDistrictStats(String district) {
        Map<String, Object> stats = new HashMap<>();

        Query query = new Query(Criteria.where("district").is(district));
        long total = mongoTemplate.count(query, Lead.class);
        stats.put("total", total);

        stats.put("demoScheduling", mongoTemplate.count(
                query.addCriteria(Criteria.where("demoScheduling").is("Yes")), Lead.class));
        stats.put("acceptedSubscription", mongoTemplate.count(
                query.addCriteria(Criteria.where("acceptedSubscription").is("Yes")), Lead.class));
        stats.put("rejected", mongoTemplate.count(
                query.addCriteria(Criteria.where("rejected").is("Yes")), Lead.class));
        stats.put("schoolClosed", mongoTemplate.count(
                query.addCriteria(Criteria.where("schoolClosed").is("Yes")), Lead.class));
        stats.put("appointmentSecured", mongoTemplate.count(
                query.addCriteria(Criteria.where("appointmentSecured").is("Yes")), Lead.class));

        return stats;
    }

    // ===================== IMPORT EXCEL - FIXED =====================
    public Map<String, Object> importExcel(List<Map<String, Object>> parsedData) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        int importedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        String currentUsername = currentUsernameOr("System");

        for (Map<String, Object> originalRow : parsedData) {
            try {
                // ------------------------------------------------------------
                // STEP 1: Normalize the row for case-insensitive column matching
                // ------------------------------------------------------------
                Map<String, Object> normalizedRow = normalizeExcelRow(originalRow);

                // ------------------------------------------------------------
                // STEP 2: Extract values using normalized keys
                // ------------------------------------------------------------
                String school = (String) normalizedRow.getOrDefault("school", "");
                String mobile = (String) normalizedRow.getOrDefault("mobile", "");
                String schoolAddress = (String) normalizedRow.getOrDefault("schooladdress", "");
                String schoolCurrentLocation = (String) normalizedRow.getOrDefault("schoolcurrentlocation", "");
                String noNumber = (String) normalizedRow.getOrDefault("nonumber", "");
                String notExistNumber = (String) normalizedRow.getOrDefault("notexistnumber", "");
                String notAnswering = (String) normalizedRow.getOrDefault("notanswering", "");
                String totalNumberOfAnswering = (String) normalizedRow.getOrDefault("totalnumberofanswering", "");
                String afternoonCallFollowup = (String) normalizedRow.getOrDefault("afternooncallrefollowup", "");
                String conveyForHeadOffice = (String) normalizedRow.getOrDefault("conveyforheadoffice", "");
                String alreadySoftwareUsers = (String) normalizedRow.getOrDefault("alreadysoftwareusers", "");
                String shareProfile = (String) normalizedRow.getOrDefault("shareprofile", "");
                String tcNextRefollowupDate = (String) normalizedRow.getOrDefault("tcnextrefollowupdate", "");
                String meNextVisitDate = (String) normalizedRow.getOrDefault("menextvisitdate", "");
                String fiftyPercentSubscription = (String) normalizedRow.getOrDefault("50subscription", "");
                String newSchoolFound = (String) normalizedRow.getOrDefault("newschoolfound", "");
                String demoScheduling = (String) normalizedRow.getOrDefault("demoschudeing", "");
                String acceptedSubscription = (String) normalizedRow.getOrDefault("acceptedsubscription", "");
                String rejected = (String) normalizedRow.getOrDefault("rejected", "");
                String schoolClosed = (String) normalizedRow.getOrDefault("schoolclosed", "");
                String numberOfNoNeed = (String) normalizedRow.getOrDefault("numberofnoneed", "");
                String cmoVisitWithMarketingExecutive = (String) normalizedRow.getOrDefault("cmovisitwithmarketingexecutive", "");
                String nextAcademyYearFollowup = (String) normalizedRow.getOrDefault("nextacademyyearrefollowuptc", "");
                String icInstalled = (String) normalizedRow.getOrDefault("icinstalled", "");
                String appointmentSecured = (String) normalizedRow.getOrDefault("appointmentsecuredbythetelecaller", "");
                String directVisit = (String) normalizedRow.getOrDefault("directvisit", "");
                String up1 = (String) normalizedRow.getOrDefault("up1", "");
                String up2 = (String) normalizedRow.getOrDefault("up2", "");
                String up3 = (String) normalizedRow.getOrDefault("up3", "");
                String up4 = (String) normalizedRow.getOrDefault("up4", "");
                String up5 = (String) normalizedRow.getOrDefault("up5", "");
                String telecallingUpdates = (String) normalizedRow.getOrDefault("telecallingupdates", "");
                String marketingTeamUpdate = (String) normalizedRow.getOrDefault("marketingteamupdate", "");

                // District - try multiple possible column names
                String district = (String) normalizedRow.getOrDefault("district", "");
                if (district == null || district.trim().isEmpty()) {
                    district = (String) normalizedRow.getOrDefault("districtname", "");
                }
                if (district == null || district.trim().isEmpty()) {
                    // Try the raw row with case-insensitive matching
                    for (String key : originalRow.keySet()) {
                        if (key != null && key.trim().toLowerCase().replaceAll("[^a-z0-9]", "").equals("district")) {
                            Object val = originalRow.get(key);
                            if (val != null) {
                                district = val.toString().trim();
                                break;
                            }
                        }
                    }
                }

                // ------------------------------------------------------------
                // STEP 3: Build the LeadRequest
                // ------------------------------------------------------------
                LeadRequest request = new LeadRequest();
                request.setSchool(school);
                request.setName(school);
                request.setMobile(mobile);
                request.setSchoolAddress(schoolAddress);
                request.setSchoolCurrentLocation(schoolCurrentLocation);
                request.setNoNumber(noNumber);
                request.setNotExistNumber(notExistNumber);
                request.setNotAnswering(notAnswering);
                request.setTotalNumberOfAnswering(totalNumberOfAnswering);
                request.setAfternoonCallFollowup(afternoonCallFollowup);
                request.setConveyForHeadOffice(conveyForHeadOffice);
                request.setAlreadySoftwareUsers(alreadySoftwareUsers);
                request.setShareProfile(shareProfile);
                request.setTcNextRefollowupDate(tcNextRefollowupDate);
                request.setMeNextVisitDate(meNextVisitDate);
                request.setFiftyPercentSubscription(fiftyPercentSubscription);
                request.setNewSchoolFound(newSchoolFound);
                request.setDemoScheduling(demoScheduling);
                request.setAcceptedSubscription(acceptedSubscription);
                request.setRejected(rejected);
                request.setSchoolClosed(schoolClosed);
                request.setNumberOfNoNeed(numberOfNoNeed);
                request.setCmoVisitWithMarketingExecutive(cmoVisitWithMarketingExecutive);
                request.setNextAcademyYearFollowup(nextAcademyYearFollowup);
                request.setIcInstalled(icInstalled);
                request.setAppointmentSecured(appointmentSecured);
                request.setDirectVisit(directVisit);
                request.setUp1(up1);
                request.setUp2(up2);
                request.setUp3(up3);
                request.setUp4(up4);
                request.setUp5(up5);

                // ========== DISTRICT-BASED AUTO-ASSIGNMENT ==========
                if (district != null && !district.trim().isEmpty()) {
                    String districtTrimmed = district.trim();
                    request.setDistrict(districtTrimmed);
                    List<Telecaller> owners = telecallerRepository.findByDistrictsContaining(districtTrimmed);
                    if (!owners.isEmpty()) {
                        request.setAssignedTo(owners.get(0).getUsername());
                        request.setAssignedDate(new Date().toString());
                    }
                }

                // Build notes
                StringBuilder notes = new StringBuilder();
                if (telecallingUpdates != null && !telecallingUpdates.isEmpty()) {
                    notes.append("Telecalling Updates: ").append(telecallingUpdates);
                }
                if (marketingTeamUpdate != null && !marketingTeamUpdate.isEmpty()) {
                    if (notes.length() > 0) notes.append("\n");
                    notes.append("Marketing Team Update: ").append(marketingTeamUpdate);
                }
                request.setNotes(notes.toString().trim());

                // Clean mobile
                String cleanMobile = mobile.replaceAll("[^0-9]", "");
                if (cleanMobile.length() == 12 && cleanMobile.startsWith("91")) {
                    cleanMobile = cleanMobile.substring(2);
                } else if (cleanMobile.length() == 11 && cleanMobile.startsWith("0")) {
                    cleanMobile = cleanMobile.substring(1);
                }
                request.setMobile(cleanMobile);

                // ------------------------------------------------------------
                // STEP 4: Save the Excel snapshot
                // This is the COMPLETE ORIGINAL Excel row (not normalized)
                // ------------------------------------------------------------
                Map<String, Object> excelSnapshot = new LinkedHashMap<>(originalRow);

                // ------------------------------------------------------------
                // STEP 5: Check if lead exists and update or create
                // ------------------------------------------------------------
                Optional<Lead> existingLead = leadRepository.findByMobile(cleanMobile);

                if (existingLead.isPresent()) {
                    // Update existing lead
                    Lead existing = existingLead.get();
                    Map<String, Object> oldExcelData = existing.getExcelData() == null 
                            ? new HashMap<>() 
                            : new HashMap<>(existing.getExcelData());

                    // Apply the update
                    LeadResponse updated = updateLead(existing.getLeadId(), request);
                    Lead saved = leadRepository.findByLeadId(updated.getLeadId()).orElse(existing);

                    // Preserve the complete Excel snapshot
                    saved.setExcelData(new LinkedHashMap<>(excelSnapshot));
                    // Keep UP1..UPn updates that telecallers added from the Call Panel
                    preserveTelecallerUpdates(oldExcelData, saved);
                    
                    // Record activity
                    recordExcelDataDiffs(saved, oldExcelData, saved.getExcelData(), currentUsername);
                    recordActivity(saved, "EXCEL_IMPORT_UPDATE", "Excel Import", "Excel row updated", 
                            null, null, null, "Excel row updated", currentUsername);
                    
                    leadRepository.save(saved);
                    updatedCount++;
                } else {
                    // Create new lead
                    request.setCategory("School");
                    request.setSource("Excel Import");
                    LeadResponse created = createLead(request);
                    Lead saved = leadRepository.findByLeadId(created.getLeadId()).orElseThrow();

                    // Preserve the complete Excel snapshot
                    saved.setExcelData(new LinkedHashMap<>(excelSnapshot));
                    
                    recordActivity(saved, "EXCEL_IMPORT", "Excel Import", "Lead imported from Excel", 
                            null, null, null, "Excel row imported", currentUsername);
                    
                    leadRepository.save(saved);
                    importedCount++;
                }

            } catch (Exception e) {
                errors.add("Error processing row: " + e.getMessage());
                skippedCount++;
            }
        }

        result.put("imported", importedCount);
        result.put("updated", updatedCount);
        result.put("skipped", skippedCount);
        result.put("errors", errors);
        return result;
    }

    // ===================== RECORD EXCEL DATA DIFFS =====================
    private void recordExcelDataDiffs(Lead lead, Map<String, Object> before, Map<String, Object> after, String updatedBy) {
        Map<String, Object> oldMap = before == null ? Collections.emptyMap() : before;
        Map<String, Object> newMap = after == null ? Collections.emptyMap() : after;
        Set<String> keys = new HashSet<>();
        keys.addAll(oldMap.keySet());
        keys.addAll(newMap.keySet());
        for (String key : keys) {
            String oldVal = String.valueOf(oldMap.get(key));
            String newVal = String.valueOf(newMap.get(key));
            if (!java.util.Objects.equals(oldVal, newVal)) {
                recordActivity(lead, "EXCEL_IMPORT_UPDATE", "Excel Import", "Excel field updated",
                        key, oldVal, newVal, "Excel value changed", updatedBy);
            }
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate localDate = cell.getDateCellValue()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return localDate.format(EXCEL_DATE_CELL_FORMAT);
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private String generateLeadId() {
        String prefix = "SCH";
        String date = new SimpleDateFormat("yyMMdd").format(new Date());
        long count = leadRepository.count() + 1;
        return prefix + date + String.format("%04d", count);
    }

    public LeadResponse convertToResponse(Lead lead) {
        LeadResponse response = new LeadResponse();

        response.setId(lead.getId());
        response.setLeadId(lead.getLeadId());
        response.setName(lead.getName());
        response.setMobile(lead.getMobile());
        response.setPhoneNumber(lead.getPhoneNumber());
        response.setEmail(lead.getEmail());
        response.setCategory(lead.getCategory());
        response.setCampaign(lead.getCampaign());
        response.setStatus(lead.getStatus());
        response.setAssignedTo(lead.getAssignedTo());
        response.setSource(lead.getSource());
        response.setLastContact(lead.getLastContact());
        response.setNotes(lead.getNotes());
        response.setDistrict(lead.getDistrict());
        response.setArea(lead.getArea());
        response.setUploadBatch(lead.getUploadBatch());
        response.setAssignedDate(lead.getAssignedDate());

        response.setSlNo(lead.getSlNo());
        response.setSchool(lead.getSchool());
        response.setSchoolAddress(lead.getSchoolAddress());
        response.setSchoolCurrentLocation(lead.getSchoolCurrentLocation());

        response.setNoNumber(lead.getNoNumber());
        response.setNotExistNumber(lead.getNotExistNumber());
        response.setNotAnswering(lead.getNotAnswering());
        response.setTotalNumberOfAnswering(lead.getTotalNumberOfAnswering());
        response.setAfternoonCallFollowup(lead.getAfternoonCallFollowup());
        response.setConveyForHeadOffice(lead.getConveyForHeadOffice());

        response.setAlreadySoftwareUsers(lead.getAlreadySoftwareUsers());
        response.setShareProfile(lead.getShareProfile());
        response.setTcNextRefollowupDate(lead.getTcNextRefollowupDate());
        response.setMeNextVisitDate(lead.getMeNextVisitDate());
        response.setFiftyPercentSubscription(lead.getFiftyPercentSubscription());
        response.setNewSchoolFound(lead.getNewSchoolFound());
        response.setDemoScheduling(lead.getDemoScheduling());
        response.setAcceptedSubscription(lead.getAcceptedSubscription());
        response.setRejected(lead.getRejected());
        response.setSchoolClosed(lead.getSchoolClosed());
        response.setNumberOfNoNeed(lead.getNumberOfNoNeed());
        response.setCmoVisitWithMarketingExecutive(lead.getCmoVisitWithMarketingExecutive());
        response.setNextAcademyYearFollowup(lead.getNextAcademyYearFollowup());
        response.setIcInstalled(lead.getIcInstalled());
        response.setAppointmentSecured(lead.getAppointmentSecured());
        response.setDirectVisit(lead.getDirectVisit());

        response.setUp1(lead.getUp1());
        response.setUp2(lead.getUp2());
        response.setUp3(lead.getUp3());
        response.setUp4(lead.getUp4());
        response.setUp5(lead.getUp5());

        response.setUpdates(lead.getUpdates());

        response.setCreatedAt(lead.getCreatedAt());
        response.setUpdatedAt(lead.getUpdatedAt());
        
        // IMPORTANT: Pass through the complete Excel data
        response.setExcelData(lead.getExcelData());
        response.setActivityHistory(lead.getActivityHistory());

        return response;
    }

    public void recordClientActivity(String leadId, String type, String action, String field, String previousValue, String newValue, String description) {
        Lead lead = leadRepository.findByLeadId(leadId).orElseThrow(() -> new RuntimeException("Lead not found with ID: "+leadId));
        assertLeadAccess(lead);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = currentUsernameOr("System");
        String source = isAdmin(auth) ? "Super Admin" : (isTelecaller(auth) ? "Telecaller" : "System");
        recordActivity(lead, type, source, action, field, previousValue, newValue, description, username);
        lead.setUpdatedAt(new Date());
        leadRepository.save(lead);
    }

    // ===================== SECURITY + AUDIT HELPERS =====================
    private boolean isTelecaller(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> {
            String r = a.getAuthority();
            return "ROLE_TELECALLER".equalsIgnoreCase(r) || "ROLE_TELE_CALLER".equalsIgnoreCase(r);
        });
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> {
            String r = a.getAuthority();
            return "ROLE_ADMIN".equalsIgnoreCase(r) || "ROLE_SUPER_ADMIN".equalsIgnoreCase(r);
        });
    }

    private String currentUsernameOr(String fallback) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : fallback;
    }

    private Telecaller getCurrentTelecaller(String username) {
        if (username == null || username.trim().isEmpty()) throw new RuntimeException("Authenticated telecaller not found");
        return telecallerRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Telecaller profile not found: " + username));
    }

    public void assertDistrictAccess(String district) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isTelecaller(auth)) return;
        Telecaller tc = getCurrentTelecaller(auth.getName());
        List<String> districts = tc.getDistricts() == null ? Collections.emptyList() : tc.getDistricts();
        boolean allowed = districts.stream().anyMatch(d -> d != null && d.trim().equalsIgnoreCase(district == null ? "" : district.trim()));
        if (!allowed) throw new org.springframework.security.access.AccessDeniedException("District is not assigned to this telecaller");
    }

    public void assertLeadAccess(Lead lead) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isTelecaller(auth)) return;
        assertDistrictAccess(lead.getDistrict());
    }

    public void recordActivity(String leadId, String type, String source, String action, String field, String previousValue, String newValue, String description, String updatedBy) {
        Lead lead = leadRepository.findByLeadId(leadId).orElseThrow(() -> new RuntimeException("Lead not found with ID: "+leadId));
        assertLeadAccess(lead);
        recordActivity(lead, type, source, action, field, previousValue, newValue, description, updatedBy);
        lead.setUpdatedAt(new Date());
        leadRepository.save(lead);
    }

    public void recordActivity(Lead lead, String type, String source, String action, String field, String previousValue, String newValue, String description, String updatedBy) {
        if (lead.getActivityHistory() == null) lead.setActivityHistory(new ArrayList<>());
        LeadActivity a = new LeadActivity();
        a.setType(type);
        a.setSource(source);
        a.setAction(action);
        a.setField(field);
        a.setPreviousValue(previousValue);
        a.setNewValue(newValue);
        a.setDescription(description);
        a.setUpdatedBy(updatedBy);
        a.setCreatedAt(new Date());
        lead.getActivityHistory().add(a);
    }

    private Map<String, String> captureLeadFields(Lead lead) {
        Map<String, String> values = new HashMap<>();
        for (Field f : Lead.class.getDeclaredFields()) {
            String n = f.getName();
            if (Set.of("id", "leadId", "createdAt", "updatedAt", "callHistory", "activityHistory").contains(n)) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(lead);
                values.put(n, String.valueOf(v));
            } catch (Exception ignored) {}
        }
        return values;
    }

    private void recordFieldDiffs(Lead lead, Map<String, String> before, String source, String type, String username) {
        Map<String, String> after = captureLeadFields(lead);
        for (Map.Entry<String, String> e : after.entrySet()) {
            String old = before.get(e.getKey()), now = e.getValue();
            if (!java.util.Objects.equals(old, now)) {
                recordActivity(lead, type, source, "Lead field updated", e.getKey(), old, now, e.getKey() + " changed", username);
            }
        }
    }

    // Accepts BOTH the Mongo _id (what the frontend sends) and the business leadId.
    private Lead resolveLead(String idOrLeadId) {
        if (idOrLeadId == null || idOrLeadId.trim().isEmpty()) return null;
        String key = idOrLeadId.trim();
        return leadRepository.findById(key)
                .or(() -> leadRepository.findByLeadId(key))
                .orElse(null);
    }

    public void deleteLead(String idOrLeadId) {
        Lead lead = resolveLead(idOrLeadId);
        if (lead == null) {
            throw new RuntimeException("Lead not found with ID: " + idOrLeadId);
        }
        leadRepository.delete(lead);
    }

    public int deleteMultipleLeads(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;

        List<Lead> toDelete = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            Lead lead = resolveLead(id);
            if (lead != null && seen.add(lead.getId())) {
                toDelete.add(lead);
            }
        }
        if (!toDelete.isEmpty()) {
            leadRepository.deleteAll(toDelete);
        }
        return toDelete.size();
    }

    public void deleteAllLeads() {
        leadRepository.deleteAll();
    }

    // ===================== SUPER ADMIN: FOLLOW-UPS OF ONE TELECALLER =====================
    // A follow-up is a Lead with tcNextRefollowupDate set (same rule the Telecaller
    // Follow-ups page uses). Leads belong to a telecaller when they sit in one of the
    // telecaller's districts or are assigned to the telecaller's username.
    public List<LeadResponse> getFollowUpsForTelecaller(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Telecaller username is required");
        }
        String uname = username.trim();
        Telecaller tc = telecallerRepository.findByUsername(uname)
                .orElseThrow(() -> new RuntimeException("Telecaller not found: " + uname));

        List<Criteria> ownerRules = new ArrayList<>();
        ownerRules.add(Criteria.where("assignedTo").is(uname));

        List<Pattern> districtPatterns = new ArrayList<>();
        if (tc.getDistricts() != null) {
            for (String d : tc.getDistricts()) {
                if (d != null && !d.trim().isEmpty()) {
                    districtPatterns.add(Pattern.compile(
                            "^" + Pattern.quote(d.trim()) + "$", Pattern.CASE_INSENSITIVE));
                }
            }
        }
        if (!districtPatterns.isEmpty()) {
            ownerRules.add(Criteria.where("district").in(districtPatterns));
        }

        Query query = new Query(new Criteria().andOperator(
                Criteria.where("tcNextRefollowupDate").nin(java.util.Arrays.asList("", null)),
                new Criteria().orOperator(ownerRules.toArray(new Criteria[0]))
        ));
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "tcNextRefollowupDate"));
        query.fields().exclude("activityHistory").exclude("callHistory");

        return mongoTemplate.find(query, Lead.class).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}