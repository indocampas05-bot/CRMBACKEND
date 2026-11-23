package com.indocrm.indocrm.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.indocrm.indocrm.dto.EmployeeDTO;
import com.indocrm.indocrm.model.CallHistory;
import com.indocrm.indocrm.model.District;
import com.indocrm.indocrm.model.Employee;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.model.UploadHistory;
import com.indocrm.indocrm.repository.DistrictRepository;
import com.indocrm.indocrm.repository.EmployeeRepository;
import com.indocrm.indocrm.repository.LeadRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;
import com.indocrm.indocrm.repository.UploadHistoryRepository;

@Service
public class SuperAdminService {

    private final TelecallerRepository telecallerRepository;
    private final DistrictRepository districtRepository;
    private final EmployeeRepository employeeRepository;
    private final LeadRepository leadRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter[] ACCEPTED_DATE_INPUT_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("dd-MM-yyyy").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd-MM-yy").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/yy").withResolverStyle(ResolverStyle.STRICT)
    };
    private static final DateTimeFormatter DATE_STORAGE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SuperAdminService(DistrictRepository districtRepository,
                             EmployeeRepository employeeRepository,
                             LeadRepository leadRepository,
                             UploadHistoryRepository uploadHistoryRepository,
                             PasswordEncoder passwordEncoder,
                             TelecallerRepository telecallerRepository) {
        this.districtRepository = districtRepository;
        this.employeeRepository = employeeRepository;
        this.leadRepository = leadRepository;
        this.uploadHistoryRepository = uploadHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.telecallerRepository = telecallerRepository;
    }

    // ==================== DISTRICT MANAGEMENT ====================

    public List<District> getAllDistricts() {
        return districtRepository.findAll();
    }

    public District getDistrictByName(String name) {
        return districtRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("District not found: " + name));
    }

    @Transactional
    public District assignDistrictToTelecaller(String districtName, String telecallerUsername) {
        District district = districtRepository.findByName(districtName)
                .orElseThrow(() -> new RuntimeException("District not found: " + districtName));

        Telecaller telecaller = telecallerRepository.findByUsername(telecallerUsername)
                .orElseThrow(() -> new RuntimeException("Telecaller not found: " + telecallerUsername));

        String previousTelecaller = district.getAssignedTo();

        if (previousTelecaller != null) {
            Telecaller previous = telecallerRepository.findByUsername(previousTelecaller).orElse(null);
            if (previous != null && previous.getDistricts() != null) {
                previous.getDistricts().remove(districtName);
                telecallerRepository.save(previous);
            }

            List<Lead> previousLeads = leadRepository.findByDistrictAndAssignedTo(districtName, previousTelecaller);
            for (Lead lead : previousLeads) {
                lead.setAssignedTo("Unassigned");
                lead.setAssignedDate(null);
                lead.setUpdatedAt(new Date());
                leadRepository.save(lead);
            }
        }

        district.setAssignedTo(telecaller.getUsername());
        district.setAssignedByName(telecaller.getFullName());
        district.setAssignedToId(telecaller.getId());
        district.setAssignedDate(new Date());
        district.setUpdatedAt(new Date());

        if (telecaller.getDistricts() == null) {
            telecaller.setDistricts(new ArrayList<>());
        }
        if (!telecaller.getDistricts().contains(districtName)) {
            telecaller.getDistricts().add(districtName);
        }
        telecaller.setUpdatedAt(new Date());
        telecallerRepository.save(telecaller);

        List<Lead> districtLeads = leadRepository.findByDistrict(districtName);
        for (Lead lead : districtLeads) {
            if (telecaller.getUsername().equals(lead.getAssignedTo())) {
                continue;
            }
            lead.setAssignedTo(telecaller.getUsername());
            lead.setAssignedDate(new Date().toString());
            lead.setUpdatedAt(new Date());
            leadRepository.save(lead);
        }

        district.setLeadCount(districtLeads.size());

        return districtRepository.save(district);
    }

    @Transactional
    public District unassignDistrict(String districtName) {
        District district = districtRepository.findByName(districtName)
                .orElseThrow(() -> new RuntimeException("District not found: " + districtName));

        if (district.getAssignedTo() != null) {
            String previousTelecaller = district.getAssignedTo();

            Telecaller telecaller = telecallerRepository.findByUsername(previousTelecaller).orElse(null);
            if (telecaller != null && telecaller.getDistricts() != null) {
                telecaller.getDistricts().remove(districtName);
                telecaller.setUpdatedAt(new Date());
                telecallerRepository.save(telecaller);
            }

            List<Lead> districtLeads = leadRepository.findByDistrict(districtName);
            for (Lead lead : districtLeads) {
                if (previousTelecaller.equals(lead.getAssignedTo())) {
                    lead.setAssignedTo("Unassigned");
                    lead.setAssignedDate(null);
                    lead.setUpdatedAt(new Date());
                    leadRepository.save(lead);
                }
            }
        }

        district.setAssignedTo(null);
        district.setAssignedByName(null);
        district.setAssignedToId(null);
        district.setAssignedDate(null);
        district.setUpdatedAt(new Date());

        return districtRepository.save(district);
    }

    @Transactional
    public District createDistrict(String name) {
        if (districtRepository.existsByName(name)) {
            throw new RuntimeException("District already exists: " + name);
        }
        District district = new District();
        district.setName(name);
        district.setLeadIds(new ArrayList<>());
        district.setLeadCount(0);
        district.setCreatedAt(new Date());
        district.setUpdatedAt(new Date());
        return districtRepository.save(district);
    }

    // ==================== TELECALLER MANAGEMENT ====================

    public List<EmployeeDTO> getAllTelecallersForAssignment() {
        List<EmployeeDTO> telecallerDTOs = new ArrayList<>();
        List<Telecaller> telecallers = telecallerRepository.findAll();

        for (Telecaller tc : telecallers) {
            if (tc.getStatus() != null && tc.getStatus().equals("Active") && tc.isEnabled()) {
                EmployeeDTO dto = new EmployeeDTO();
                dto.setId(tc.getId());
                dto.setUsername(tc.getUsername());
                dto.setFullName(tc.getFullName());
                dto.setEmail(tc.getEmail());
                dto.setPhone(tc.getMobile());
                dto.setRole(tc.getRole() != null ? tc.getRole() : "Telecaller");
                dto.setEnabled(true);
                telecallerDTOs.add(dto);
            }
        }

        return telecallerDTOs;
    }

    public List<EmployeeDTO> getAllEmployees() {
        return getAllTelecallersForAssignment();
    }

    public EmployeeDTO getEmployeeByUsername(String username) {
        Telecaller telecaller = telecallerRepository.findByUsername(username).orElse(null);
        if (telecaller != null) {
            EmployeeDTO dto = new EmployeeDTO();
            dto.setId(telecaller.getId());
            dto.setUsername(telecaller.getUsername());
            dto.setFullName(telecaller.getFullName());
            dto.setEmail(telecaller.getEmail());
            dto.setPhone(telecaller.getMobile());
            dto.setRole(telecaller.getRole() != null ? telecaller.getRole() : "Telecaller");
            if (telecaller.getDistricts() != null && !telecaller.getDistricts().isEmpty()) {
                dto.setAssignedDistrict(telecaller.getDistricts().get(0));
            }
            dto.setEnabled(telecaller.isEnabled());
            return dto;
        }

        Employee employee = employeeRepository.findByUsername(username).orElse(null);
        if (employee != null) {
            return convertToDTO(employee);
        }

        throw new RuntimeException("User not found: " + username);
    }

    @Transactional
    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByUsername(employee.getUsername())) {
            throw new RuntimeException("Username already exists: " + employee.getUsername());
        }
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Email already exists: " + employee.getEmail());
        }

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setRole(employee.getRole() != null ? employee.getRole() : "EMPLOYEE");
        employee.setEnabled(true);
        employee.setCreatedAt(new Date());
        employee.setUpdatedAt(new Date());

        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(String id, Employee employee) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        existing.setFullName(employee.getFullName());
        existing.setEmail(employee.getEmail());
        existing.setPhone(employee.getPhone());
        existing.setRole(employee.getRole());
        existing.setEnabled(employee.isEnabled());
        existing.setUpdatedAt(new Date());

        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(employee.getPassword()));
        }

        return employeeRepository.save(existing);
    }

    @Transactional
    public void deleteEmployee(String id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (employee.getAssignedDistrict() != null) {
            District district = districtRepository.findByName(employee.getAssignedDistrict()).orElse(null);
            if (district != null) {
                district.setAssignedTo(null);
                district.setAssignedByName(null);
                district.setAssignedToId(null);
                district.setAssignedDate(null);
                district.setUpdatedAt(new Date());
                districtRepository.save(district);
            }
        }

        employeeRepository.deleteById(id);
    }

    private EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setUsername(employee.getUsername());
        dto.setFullName(employee.getFullName());
        dto.setEmail(employee.getEmail());
        dto.setPhone(employee.getPhone());
        dto.setRole(employee.getRole());
        dto.setAssignedDistrict(employee.getAssignedDistrict());
        dto.setEnabled(employee.isEnabled());
        return dto;
    }

    // ==================== LEAD IMPORT – FULLY FIXED ====================

    @Transactional
    public UploadHistory importLeadsFromExcel(Map<String, Object> importData, String fileName, String uploadedBy) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int updated = 0;

        UploadHistory history = new UploadHistory();
        history.setFileName(fileName);
        history.setUploadedBy(uploadedBy);
        history.setUploadedAt(new Date());
        history.setStatus("SUCCESS");
        history.setErrors(new ArrayList<>());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) importData.get("leads");
        String uploadDistrict = (String) importData.get("district");
        if (rows == null || rows.isEmpty()) throw new RuntimeException("No data to import");

        history.setTotalRecords(rows.size());
        String batchId = "BATCH-" + System.currentTimeMillis();
        java.util.Map<String, District> districtCache = new HashMap<>();
        java.util.Map<String, String> ownerCache = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int rowNumber = i + 2;
            try {
                Map<String, Object> incoming = normalizeExcelRow(row);
                String mobile = cleanPhoneNumber(extractValue(incoming, "mobile"));
                String suppliedLeadId = firstNonEmpty(extractValue(incoming, "leadid"), extractValue(incoming, "id"));
                String school = firstNonEmpty(extractValue(incoming, "school"), extractValue(incoming, "schooltotal277"), extractValue(incoming, "schooltotal"), extractValue(incoming, "name"));
                if (school == null) school = "Unknown School " + rowNumber;

                Lead existing = null;
                if (suppliedLeadId != null) existing = leadRepository.findByLeadId(suppliedLeadId).orElse(null);
                if (existing == null && mobile != null) existing = leadRepository.findByMobile(mobile).orElse(null);
                // Important for a phone-number change: match the same school/name when the new phone is different.
                if (existing == null && school != null) {
                    final String schoolKey = school.trim();
                    existing = leadRepository.findAll().stream()
                            .filter(l -> l.getSchool() != null && l.getSchool().trim().equalsIgnoreCase(schoolKey))
                            .findFirst().orElse(null);
                }

                String rowDistrict = firstNonEmpty(extractValue(incoming, "district"), extractValue(incoming, "districtname"), uploadDistrict, existing != null ? existing.getDistrict() : null, "Unknown");
                rowDistrict = rowDistrict.trim();

                District district = districtCache.computeIfAbsent(rowDistrict, d -> districtRepository.findByName(d).orElseGet(() -> {
                    District nd = new District();
                    nd.setName(d);
                    nd.setLeadIds(new ArrayList<>());
                    nd.setLeadCount(0);
                    nd.setCreatedAt(new Date());
                    nd.setUpdatedAt(new Date());
                    return districtRepository.save(nd);
                }));
                String owner = ownerCache.computeIfAbsent(rowDistrict, d -> {
                    List<Telecaller> owners = telecallerRepository.findByDistrictsContaining(d);
                    return owners.isEmpty() ? "Unassigned" : owners.get(0).getUsername();
                });

                if (existing == null) {
                    Lead lead = new Lead();
                    lead.setLeadId(firstNonEmpty(suppliedLeadId, generateLeadId()));
                    lead.setCreatedAt(new Date());
                    lead.setActivityHistory(new ArrayList<>());
                    lead.setCallHistory(new CallHistory());
                    lead.setStatus("New");
                    lead.setSource("Excel Import");
                    lead.setCategory("School");
                    lead.setAssignedTo(owner);
                    lead.setUploadBatch(batchId);
                    applyExcelValues(lead, incoming, rowDistrict, owner, batchId, true);
                    lead.setExcelData(new HashMap<>(incoming));
                    lead.setUpdatedAt(new Date());
                    lead = leadRepository.save(lead);

                    addActivity(lead, "EXCEL_IMPORT", "Excel Import", "Lead imported from Excel", null, null, null,
                            "New lead imported from " + fileName + " (row " + rowNumber + ")", uploadedBy);
                    leadRepository.save(lead);

                    if (!district.getLeadIds().contains(lead.getId())) district.getLeadIds().add(lead.getId());
                    district.setLeadCount(district.getLeadIds().size());
                    district.setUpdatedAt(new Date());
                    districtRepository.save(district);
                    imported++;
                } else {
                    assertDistrictOwnershipForImport(existing, rowDistrict);
                    Map<String, Object> oldExcel = existing.getExcelData() == null ? new HashMap<>() : new HashMap<>(existing.getExcelData());
                    Map<String, String> before = captureMappedFields(existing);

                    applyExcelValues(existing, incoming, rowDistrict, owner, batchId, false);
                    Map<String, Object> mergedExcel = mergeNonBlank(oldExcel, incoming);
                    existing.setExcelData(mergedExcel);
                    existing.setUploadBatch(batchId);
                    existing.setUpdatedAt(new Date());
                    Lead saved = leadRepository.save(existing);

                    // Compare every Excel column, not only the mapped CRM columns.
                    for (Map.Entry<String, Object> entry : mergedExcel.entrySet()) {
                        String key = entry.getKey();
                        String oldValue = stringify(oldExcel.get(key));
                        String newValue = stringify(entry.getValue());
                        if (!sameValue(oldValue, newValue)) {
                            addActivity(saved, "EXCEL_IMPORT_UPDATE", "Excel Import", "Excel field updated",
                                    key, oldValue, newValue,
                                    "Excel import changed field '" + key + "'", uploadedBy);
                        }
                    }
                    // Also catch changes to mapped fields when the old record predates excelData.
                    Map<String, String> after = captureMappedFields(saved);
                    for (Map.Entry<String, String> e : after.entrySet()) {
                        String oldValue = before.get(e.getKey());
                        if (!sameValue(oldValue, e.getValue()) && !hasRawActivityForField(saved, e.getKey(), oldValue, e.getValue())) {
                            addActivity(saved, "EXCEL_IMPORT_UPDATE", "Excel Import", "Lead field updated",
                                    e.getKey(), oldValue, e.getValue(), "Mapped lead field changed during Excel import", uploadedBy);
                        }
                    }
                    addActivity(saved, "EXCEL_IMPORT_BATCH", "Excel Import", "Excel import processed",
                            null, null, null, "Existing lead checked and updated from " + fileName + " (row " + rowNumber + ")", uploadedBy);
                    leadRepository.save(saved);
                    updated++;
                }
            } catch (Exception ex) {
                errors.add("Row " + rowNumber + ": " + ex.getMessage());
            }
        }

        history.setImportedRecords(imported);
        if (!errors.isEmpty()) {
            history.setStatus("PARTIAL");
            history.setErrors(errors);
        }
        // Existing UploadHistory model uses importedRecords; keep that field authoritative.
        return uploadHistoryRepository.save(history);
    }

    private Map<String, Object> normalizeExcelRow(Map<String, Object> row) {
        Map<String, Object> normalized = new java.util.LinkedHashMap<>();
        if (row == null) return normalized;
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey() == null) continue;
            String key = e.getKey().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            normalized.put(key, e.getValue());
        }
        return normalized;
    }

    private Map<String, Object> mergeNonBlank(Map<String, Object> oldData, Map<String, Object> incoming) {
        Map<String, Object> merged = new java.util.LinkedHashMap<>();
        if (oldData != null) merged.putAll(oldData);
        if (incoming != null) {
            for (Map.Entry<String, Object> e : incoming.entrySet()) {
                if (e.getValue() != null && !e.getValue().toString().trim().isEmpty()) merged.put(e.getKey(), e.getValue());
                else if (!merged.containsKey(e.getKey())) merged.put(e.getKey(), e.getValue());
            }
        }
        return merged;
    }

    private void applyExcelValues(Lead lead, Map<String, Object> row, String district, String owner, String batchId, boolean isNew) {
        String value;
        value = extractValue(row, "slno"); if (isNonEmpty(value)) { try { lead.setSlNo(Integer.parseInt(value.replaceAll("\\.0$", ""))); } catch (Exception ignored) {} }
        value = extractValue(row, "school"); if (isNonEmpty(value)) lead.setSchool(value);
        value = extractValue(row, "name"); if (isNonEmpty(value)) lead.setName(value); else if (isNonEmpty(lead.getSchool())) lead.setName(lead.getSchool());
        value = extractValue(row, "mobile"); if (isNonEmpty(value)) { String m = cleanPhoneNumber(value); if (m != null) { lead.setMobile(m); lead.setPhoneNumber(m); } }
        value = extractValue(row, "phonenumber"); if (isNonEmpty(value)) lead.setPhoneNumber(cleanPhoneNumber(value));
        value = extractValue(row, "email"); if (isNonEmpty(value)) lead.setEmail(value);
        value = extractValue(row, "category"); if (isNonEmpty(value)) lead.setCategory(value);
        value = extractValue(row, "campaign"); if (isNonEmpty(value)) lead.setCampaign(value);
        value = extractValue(row, "status"); if (isNonEmpty(value)) lead.setStatus(value);
        value = extractValue(row, "source"); if (isNonEmpty(value)) lead.setSource(value);
        value = extractValue(row, "notes"); if (isNonEmpty(value)) lead.setNotes(value);
        value = extractValue(row, "district"); if (!isNonEmpty(value)) value = extractValue(row, "districtname"); if (isNonEmpty(value)) lead.setDistrict(value.trim()); else if (isNonEmpty(district)) lead.setDistrict(district);
        value = extractValue(row, "area"); if (isNonEmpty(value)) lead.setArea(value);
        value = extractValue(row, "schooladdress"); if (isNonEmpty(value)) lead.setSchoolAddress(value);
        value = extractValue(row, "schoolcurrentlocation"); if (isNonEmpty(value)) lead.setSchoolCurrentLocation(value);

        setIfPresent(row, "nonumber", v -> lead.setNoNumber(v));
        setIfPresent(row, "notexistnumber", v -> lead.setNotExistNumber(v));
        setIfPresent(row, "notanswering", v -> lead.setNotAnswering(v));
        setIfPresent(row, "totalnumberofanswering", v -> lead.setTotalNumberOfAnswering(v));
        setIfPresent(row, "afternooncallrefollowup", v -> lead.setAfternoonCallFollowup(v));
        setIfPresent(row, "conveyforheadoffice", v -> lead.setConveyForHeadOffice(v));
        setIfPresent(row, "alreadysoftwareusers", v -> lead.setAlreadySoftwareUsers(v));
        setIfPresent(row, "shareprofile", v -> lead.setShareProfile(v));
        setIfPresent(row, "tcnextrefollowupdate", v -> lead.setTcNextRefollowupDate(v));
        setIfPresent(row, "menextvisitdate", v -> lead.setMeNextVisitDate(v));
        setIfPresent(row, "50subscription", v -> lead.setFiftyPercentSubscription(v));
        setIfPresent(row, "newschoolfound", v -> lead.setNewSchoolFound(v));
        setIfPresent(row, "demoschudeing", v -> lead.setDemoScheduling(v));
        setIfPresent(row, "demoscheduling", v -> lead.setDemoScheduling(v));
        setIfPresent(row, "acceptedsubscription", v -> lead.setAcceptedSubscription(v));
        setIfPresent(row, "rejected", v -> lead.setRejected(v));
        setIfPresent(row, "schoolclosed", v -> lead.setSchoolClosed(v));
        setIfPresent(row, "numberofnoneed", v -> lead.setNumberOfNoNeed(v));
        setIfPresent(row, "cmovisitwithmarketingexecutive", v -> lead.setCmoVisitWithMarketingExecutive(v));
        setIfPresent(row, "nextacademyyearrefollowuptc", v -> lead.setNextAcademyYearFollowup(v));
        setIfPresent(row, "icinstalled", v -> lead.setIcInstalled(v));
        setIfPresent(row, "appointmentsecuredbythetelecaller", v -> lead.setAppointmentSecured(v));
        setIfPresent(row, "directvisit", v -> lead.setDirectVisit(v));
        setIfPresent(row, "up1", v -> lead.setUp1(v)); setIfPresent(row, "up2", v -> lead.setUp2(v));
        setIfPresent(row, "up3", v -> lead.setUp3(v)); setIfPresent(row, "up4", v -> lead.setUp4(v)); setIfPresent(row, "up5", v -> lead.setUp5(v));
        setIfPresent(row, "uploadbatch", v -> lead.setUploadBatch(v));
        setIfPresent(row, "assigneddate", v -> lead.setAssignedDate(v));

        List<String> updates = new ArrayList<>();
        for (String key : Arrays.asList("up1","up2","up3","up4","up5")) { String v = extractValue(row,key); if (isNonEmpty(v)) updates.add(v); }
        String teleUpdates = extractValue(row, "telecallingupdates");
        if (updates.isEmpty() && isNonEmpty(teleUpdates)) updates.addAll(parseTelecallingUpdates(teleUpdates));
        if (!updates.isEmpty()) lead.setUpdates(updates);

        if (isNew) {
            lead.setCategory(isNonEmpty(lead.getCategory()) ? lead.getCategory() : "School");
            lead.setSource(isNonEmpty(lead.getSource()) ? lead.getSource() : "Excel Import");
            lead.setStatus(isNonEmpty(lead.getStatus()) ? lead.getStatus() : "New");
        }
        if (isNonEmpty(district)) lead.setDistrict(district);
        if (isNonEmpty(owner) && !"Unassigned".equals(owner)) { lead.setAssignedTo(owner); lead.setAssignedDate(new Date().toString()); }
        if (!isNonEmpty(lead.getUploadBatch())) lead.setUploadBatch(batchId);
        if (!isNonEmpty(lead.getLastContact())) lead.setLastContact("--");
        if (lead.getCreatedAt() == null) lead.setCreatedAt(new Date());
    }

    private interface ValueSetter { void set(String value); }
    private void setIfPresent(Map<String,Object> row, String key, ValueSetter setter) { String v=extractValue(row,key); if(isNonEmpty(v)) setter.set(v); }

    private Map<String,String> captureMappedFields(Lead l) {
        Map<String,String> m = new java.util.LinkedHashMap<>();
        m.put("name", l.getName()); m.put("mobile", l.getMobile()); m.put("phoneNumber", l.getPhoneNumber()); m.put("email", l.getEmail());
        m.put("status", l.getStatus()); m.put("district", l.getDistrict()); m.put("area", l.getArea()); m.put("school", l.getSchool());
        m.put("schoolAddress", l.getSchoolAddress()); m.put("schoolCurrentLocation", l.getSchoolCurrentLocation());
        m.put("tcNextRefollowupDate", l.getTcNextRefollowupDate()); m.put("meNextVisitDate", l.getMeNextVisitDate());
        m.put("assignedTo", l.getAssignedTo()); m.put("notes", l.getNotes()); m.put("source", l.getSource());
        m.put("campaign", l.getCampaign()); m.put("category", l.getCategory());
        m.put("up1", l.getUp1()); m.put("up2", l.getUp2()); m.put("up3", l.getUp3()); m.put("up4", l.getUp4()); m.put("up5", l.getUp5());
        return m;
    }

    private void addActivity(Lead lead, String type, String source, String action, String field, String oldValue, String newValue, String description, String user) {
        if (lead.getActivityHistory() == null) lead.setActivityHistory(new ArrayList<>());
        com.indocrm.indocrm.model.LeadActivity a = new com.indocrm.indocrm.model.LeadActivity();
        a.setId(java.util.UUID.randomUUID().toString()); a.setType(type); a.setSource(source); a.setAction(action); a.setField(field);
        a.setPreviousValue(oldValue); a.setNewValue(newValue); a.setDescription(description); a.setUpdatedBy(user); a.setCreatedAt(new Date());
        lead.getActivityHistory().add(a);
    }

    private boolean hasRawActivityForField(Lead lead, String field, String oldValue, String newValue) {
        if (lead.getActivityHistory() == null) return false;
        return lead.getActivityHistory().stream().anyMatch(a -> field.equalsIgnoreCase(String.valueOf(a.getField()))
                && sameValue(oldValue, a.getPreviousValue()) && sameValue(newValue, a.getNewValue()));
    }

    private boolean sameValue(String a, String b) { return java.util.Objects.equals(a == null ? "" : a.trim(), b == null ? "" : b.trim()); }
    private String stringify(Object o) { return o == null ? null : String.valueOf(o); }
    private String firstNonEmpty(String... values) { for(String v: values) if(isNonEmpty(v)) return v.trim(); return null; }

    private void assertDistrictOwnershipForImport(Lead lead, String district) {
        // Super Admin performs the import; existing lead may legitimately move districts.
        // No telecaller security check is applied here.
    }

    // ==================== HELPER METHODS ====================

    private String extractValue(Map<String, Object> row, String key) {
        if (row == null || key == null) return null;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                Object value = entry.getValue();
                if (value == null) return null;
                String str = value.toString().trim();
                return str.isEmpty() ? null : str;
            }
        }
        return null;
    }

    private boolean isNonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String cleanPhoneNumber(String phone) {
        if (phone == null) return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() == 10) {
            return cleaned;
        } else if (cleaned.length() == 12 && cleaned.startsWith("91")) {
            return cleaned.substring(2);
        } else if (cleaned.length() == 11 && cleaned.startsWith("0")) {
            return cleaned.substring(1);
        }
        return cleaned.length() >= 10 ? cleaned : null;
    }

    private String normalizeFollowUpDate(String rawValue, int excelRowNumber, String columnName, List<String> errors) {
        if (rawValue == null) return null;
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) return null;

        String dateStr = trimmed;
        if (trimmed.matches(".*\\d{2}[/-]\\d{2}[/-]\\d{2,4}.*")) {
            dateStr = trimmed.replaceAll(".*?(\\d{2}[/-]\\d{2}[/-]\\d{2,4}).*", "$1");
        }

        for (DateTimeFormatter formatter : ACCEPTED_DATE_INPUT_FORMATS) {
            try {
                LocalDate parsed = LocalDate.parse(dateStr, formatter);
                return parsed.format(DATE_STORAGE_FORMAT);
            } catch (DateTimeParseException ignored) {
            }
        }

        errors.add("Invalid date format at Row " + excelRowNumber + ", Column " + columnName + " (value: " + trimmed + ")");
        return null;
    }

    /**
     * Enhanced parser: splits telecalling updates into multiple entries.
     * Strategies: newlines, date patterns, keywords (REF, TRY AGAIN, REFOLLOWUP),
     * and common separators (|, ;, /, double spaces).
     */
    private List<String> parseTelecallingUpdates(String text) {
        List<String> updates = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return updates;
        }

        // Normalize line breaks
        String normalized = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // ---- Strategy 1: Split by newline ----
        String[] lines = normalized.split("\n");
        List<String> candidates = Arrays.stream(lines)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (candidates.size() > 1) {
            List<String> result = new ArrayList<>();
            for (String line : candidates) {
                result.addAll(splitBySeparators(line));
            }
            updates = result.stream()
                    .filter(s -> !s.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (!updates.isEmpty()) {
                return updates;
            }
        }

        // ---- Strategy 2: Split by date patterns (dd/mm/yy etc.) ----
        String[] dateSplit = normalized.split("(?=\\d{2}[/-]\\d{2}[/-]\\d{2,4})");
        if (dateSplit.length > 1) {
            List<String> dateParts = Arrays.stream(dateSplit)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            List<String> result = new ArrayList<>();
            for (String part : dateParts) {
                result.addAll(splitBySeparators(part));
            }
            updates = result.stream()
                    .filter(s -> !s.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (!updates.isEmpty()) {
                return updates;
            }
        }

        // ---- Strategy 3: Split by keywords (REF, TRY AGAIN, REFOLLOWUP) ----
        String[] refSplit = normalized.split("(?i)(?=REF|TRY AGAIN|REFOLLOWUP)");
        if (refSplit.length > 1) {
            List<String> refParts = Arrays.stream(refSplit)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            List<String> result = new ArrayList<>();
            for (String part : refParts) {
                result.addAll(splitBySeparators(part));
            }
            updates = result.stream()
                    .filter(s -> !s.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (!updates.isEmpty()) {
                return updates;
            }
        }

        // ---- Strategy 4: Split by common separators (|, ;, /, double space) ----
        List<String> sepSplit = splitBySeparators(normalized);
        if (sepSplit.size() > 1) {
            updates = sepSplit.stream()
                    .filter(s -> !s.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (!updates.isEmpty()) {
                return updates;
            }
        }

        // ---- Fallback: treat as a single entry ----
        updates.add(normalized.trim());
        return updates;
    }

    /**
     * Splits a string by common separators: |, ;, /, two or more spaces.
     */
    private List<String> splitBySeparators(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        String[] separators = { " \\| ", " ; ", " / ", "  " };
        for (String sep : separators) {
            if (text.contains(sep)) {
                String[] parts = text.split(sep);
                return Arrays.stream(parts)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
            }
        }
        // If no separator, return as single element
        return List.of(text.trim());
    }

    // ==================== UPLOAD HISTORY ====================

    public List<UploadHistory> getUploadHistory() {
        return uploadHistoryRepository.findAllByOrderByUploadedAtDesc();
    }

    public UploadHistory getUploadHistoryById(String id) {
        return uploadHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Upload history not found"));
    }

    // ==================== DASHBOARD STATS ====================

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDistricts", districtRepository.count());
        stats.put("totalEmployees", telecallerRepository.count());
        stats.put("totalLeads", leadRepository.count());
        stats.put("totalUploads", uploadHistoryRepository.count());
        return stats;
    }

    // ==================== LEAD ID GENERATOR ====================

    private String generateLeadId() {
        List<Lead> allLeads = leadRepository.findAll();
        int maxId = 0;
        for (Lead lead : allLeads) {
            String leadId = lead.getLeadId();
            if (leadId != null && leadId.startsWith("LD-")) {
                try {
                    int num = Integer.parseInt(leadId.substring(3));
                    if (num > maxId) maxId = num;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int nextId = Math.max(maxId + 1, 5277);
        return String.format("LD-%04d", nextId);
    }
}

