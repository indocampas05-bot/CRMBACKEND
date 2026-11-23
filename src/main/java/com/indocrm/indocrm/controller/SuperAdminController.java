// E:\indocrm\src\main\java\com\indocrm\indocrm\controller\SuperAdminController.java
// FIXED VERSION - Proper Excel Import Handling

package com.indocrm.indocrm.controller;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.indocrm.indocrm.dto.DistrictAssignmentDTO;
import com.indocrm.indocrm.dto.EmployeeDTO;
import com.indocrm.indocrm.model.District;
import com.indocrm.indocrm.model.Employee;
import com.indocrm.indocrm.model.UploadHistory;
import com.indocrm.indocrm.service.SuperAdminService;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    // ==================== DISTRICT ENDPOINTS ====================

    @GetMapping("/districts")
    public ResponseEntity<List<District>> getAllDistricts() {
        try {
            List<District> districts = superAdminService.getAllDistricts();
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch districts: " + e.getMessage());
        }
    }

    @GetMapping("/districts/{name}")
    public ResponseEntity<District> getDistrictByName(@PathVariable String name) {
        return ResponseEntity.ok(superAdminService.getDistrictByName(name));
    }

    @PostMapping("/districts")
    public ResponseEntity<District> createDistrict(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("District name is required");
        }
        District district = superAdminService.createDistrict(name.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(district);
    }

    @PostMapping("/districts/assign")
    public ResponseEntity<District> assignDistrict(@RequestBody DistrictAssignmentDTO assignment) {
        District district = superAdminService.assignDistrictToTelecaller(
                assignment.getDistrictName(),
                assignment.getEmployeeUsername()
        );
        return ResponseEntity.ok(district);
    }

    @PostMapping("/districts/unassign/{name}")
    public ResponseEntity<District> unassignDistrict(@PathVariable String name) {
        District district = superAdminService.unassignDistrict(name);
        return ResponseEntity.ok(district);
    }

    // ==================== TELECALLER ENDPOINTS ====================

    @GetMapping("/telecallers")
    public ResponseEntity<List<EmployeeDTO>> getTelecallersForAssignment() {
        try {
            List<EmployeeDTO> telecallers = superAdminService.getAllTelecallersForAssignment();
            return ResponseEntity.ok(telecallers);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch telecallers: " + e.getMessage());
        }
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        try {
            List<EmployeeDTO> telecallers = superAdminService.getAllTelecallersForAssignment();
            return ResponseEntity.ok(telecallers);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch employees: " + e.getMessage());
        }
    }

    @GetMapping("/employees/{username}")
    public ResponseEntity<EmployeeDTO> getEmployee(@PathVariable String username) {
        return ResponseEntity.ok(superAdminService.getEmployeeByUsername(username));
    }

    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        Employee created = superAdminService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable String id, @RequestBody Employee employee) {
        Employee updated = superAdminService.updateEmployee(id, employee);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Map<String, String>> deleteEmployee(@PathVariable String id) {
        superAdminService.deleteEmployee(id);
        return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
    }

    // ==================== IMPORT ENDPOINTS - FIXED ====================

    @PostMapping("/import-leads")
    public ResponseEntity<UploadHistory> importLeads(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy,
            @RequestParam(value = "district", required = false) String district) {

        System.out.println("📁 Received import request: " + file.getOriginalFilename() + " by " + uploadedBy);
        
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                throw new RuntimeException("Invalid file name");
            }
            
            String lowerName = fileName.toLowerCase();
            if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
                throw new RuntimeException("Please upload .xlsx or .xls file only. Received: " + fileName);
            }
            
            List<Map<String, Object>> leadData = parseExcelFile(file);
            
            if (leadData.isEmpty()) {
                throw new RuntimeException("No data found in file. Please check the file format.");
            }
            
            Map<String, Object> importData = new HashMap<>();
            importData.put("leads", leadData);
            importData.put("district", district);
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actualUploadedBy = uploadedBy;
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getName() != null
                    && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
                actualUploadedBy = authentication.getName();
            }

            UploadHistory history = superAdminService.importLeadsFromExcel(
                    importData,
                    file.getOriginalFilename(),
                    actualUploadedBy
            );
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            System.err.println("❌ Import failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process file: " + e.getMessage());
        }
    }

    // ==================== UPLOAD HISTORY ENDPOINTS ====================

    @GetMapping("/upload-history")
    public ResponseEntity<List<UploadHistory>> getUploadHistory() {
        return ResponseEntity.ok(superAdminService.getUploadHistory());
    }

    @GetMapping("/upload-history/{id}")
    public ResponseEntity<UploadHistory> getUploadHistoryById(@PathVariable String id) {
        return ResponseEntity.ok(superAdminService.getUploadHistoryById(id));
    }

    // ==================== DASHBOARD STATS ENDPOINTS ====================

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(superAdminService.getDashboardStats());
    }

    @GetMapping("/district-stats")
    public ResponseEntity<List<District>> getDistrictStats() {
        return ResponseEntity.ok(superAdminService.getAllDistricts());
    }

    // ==================== HELPER METHODS ====================

    private List<Map<String, Object>> parseExcelFile(MultipartFile file) throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new RuntimeException("Excel file is empty");
            }

            // Parse headers from first row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Header row not found in Excel file");
            }

            for (Cell cell : headerRow) {
                String headerValue = getCellStringValue(cell);
                if (headerValue != null && !headerValue.trim().isEmpty()) {
                    headers.add(headerValue.trim().toLowerCase().replaceAll("[^a-zA-Z0-9]", ""));
                }
            }

            System.out.println("📋 Found headers: " + headers);

            // Check for required columns
            boolean hasSchool = headers.stream().anyMatch(h -> h.contains("school"));
            boolean hasMobile = headers.stream().anyMatch(h -> h.contains("mobile"));

            if (!hasSchool || !hasMobile) {
                throw new RuntimeException("Missing required columns: 'School' and/or 'Mobile'. Found headers: " + headers);
            }

            // Find column indices
            int schoolCol = findColumnIndex(headers, "school");
            int mobileCol = findColumnIndex(headers, "mobile");

            // Parse data rows
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, Object> rowMap = new HashMap<>();
                boolean hasData = false;

                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c);
                    String value = cell != null ? getCellStringValue(cell) : "";
                    if (value != null && !value.isBlank()) hasData = true;
                    rowMap.put(headers.get(c), value);
                }

                if (hasData) {
                    // Ensure school and mobile are present
                    String school = schoolCol >= 0 ? (String) rowMap.get(headers.get(schoolCol)) : "";
                    String mobile = mobileCol >= 0 ? (String) rowMap.get(headers.get(mobileCol)) : "";
                    
                    if (school == null || school.trim().isEmpty()) {
                        rowMap.put("school", "Unknown School");
                    }
                    if (mobile == null || mobile.trim().isEmpty()) {
                        rowMap.put("mobile", "");
                    }
                    
                    data.add(rowMap);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Excel parsing error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        System.out.println("✅ Parsed " + data.size() + " rows from Excel file");
        return data;
    }

    private int findColumnIndex(List<String> headers, String keyword) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).contains(keyword)) {
                return i;
            }
        }
        return -1;
    }

    private static final DateTimeFormatter EXCEL_DATE_CELL_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        LocalDate localDate = cell.getDateCellValue()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        return localDate.format(EXCEL_DATE_CELL_FORMAT);
                    }
                    double num = cell.getNumericCellValue();
                    if (num == Math.floor(num) && !Double.isInfinite(num)) {
                        return String.valueOf((long) num);
                    }
                    return String.valueOf(num);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue().trim();
                    } catch (Exception e) {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}
