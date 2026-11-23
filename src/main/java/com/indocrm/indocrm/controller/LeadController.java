package com.indocrm.indocrm.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.indocrm.indocrm.dto.LeadRequest;
import com.indocrm.indocrm.dto.LeadResponse;
import com.indocrm.indocrm.model.LeadActivity;
import com.indocrm.indocrm.service.LeadService;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    @Autowired
    private LeadService leadService;

    // ============================================================
    // CREATE LEAD
    // ============================================================

    @PostMapping
    public ResponseEntity<LeadResponse> createLead(
            @RequestBody LeadRequest request) {

        LeadResponse response = leadService.createLead(request);

        return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
    }

    // ============================================================
    // UPDATE LEAD
    // ============================================================

    @PutMapping("/{leadId}")
    public ResponseEntity<LeadResponse> updateLead(
            @PathVariable String leadId,
            @RequestBody LeadRequest request) {

        LeadResponse response =
                leadService.updateLead(leadId, request);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET SINGLE LEAD
    // ============================================================

    @GetMapping("/{leadId}")
    public ResponseEntity<LeadResponse> getLeadById(
            @PathVariable String leadId) {

        LeadResponse response =
                leadService.getLeadById(leadId);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET LEADS BY DISTRICT
    // ============================================================

    @GetMapping("/district/{district}")
    public ResponseEntity<List<LeadResponse>> getLeadsByDistrict(
            @PathVariable String district) {

        return ResponseEntity.ok(
                leadService.getLeadsByDistrict(district)
        );
    }

    // ============================================================
    // TELECALLER - MY DISTRICTS
    // ============================================================

    @GetMapping("/my-districts")
    public ResponseEntity<List<Map<String, Object>>> getMyDistricts() {

        return ResponseEntity.ok(
                leadService.getMyDistricts()
        );
    }

    // ============================================================
    // TELECALLER - MY LEADS BY DISTRICT
    // ============================================================

    @GetMapping("/my-districts/{district}")
    public ResponseEntity<List<LeadResponse>> getMyLeadsByDistrict(
            @PathVariable String district) {

        return ResponseEntity.ok(
                leadService.getMyLeadsByDistrict(district)
        );
    }

    // ============================================================
    // EXCEL IMPORT
    //
    // THIS IS THE IMPORTANT ENDPOINT
    // ============================================================

    @PostMapping(
            value = "/import-excel",
            consumes = {
                    "multipart/form-data"
            }
    )
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> importExcel(
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {

            // ----------------------------------------------------
            // Validate file
            // ----------------------------------------------------

            if (file == null || file.isEmpty()) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Please upload an Excel file."
                );

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            String originalFilename =
                    file.getOriginalFilename();

            if (originalFilename == null ||
                    originalFilename.trim().isEmpty()) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Invalid file name."
                );

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            String lowerName =
                    originalFilename.toLowerCase();

            if (!lowerName.endsWith(".xlsx") &&
                    !lowerName.endsWith(".xls")) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "Only .xlsx and .xls files are supported."
                );

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            // ----------------------------------------------------
            // Parse complete Excel
            // ----------------------------------------------------

            List<Map<String, Object>> rows =
                    parseExcelFile(file);

            if (rows == null || rows.isEmpty()) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "message",
                        "No records found in the Excel file."
                );

                return ResponseEntity
                        .badRequest()
                        .body(response);
            }

            // ----------------------------------------------------
            // Import into MongoDB
            // ----------------------------------------------------

            Map<String, Object> result =
                    leadService.importExcel(rows);

            result.put(
                    "success",
                    true
            );

            result.put(
                    "fileName",
                    originalFilename
            );

            result.put(
                    "totalRows",
                    rows.size()
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            e.printStackTrace();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "Excel import failed."
            );

            response.put(
                    "error",
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);
        }
    }

    // ============================================================
    // COMPLETE EXCEL PARSER
    //
    // IMPORTANT:
    // We keep every Excel column.
    // We don't throw away unknown columns.
    // ============================================================

    private List<Map<String, Object>> parseExcelFile(
            MultipartFile file) throws Exception {

        List<Map<String, Object>> result =
                new ArrayList<>();

        try (
                InputStream inputStream =
                        file.getInputStream();

                Workbook workbook =
                        WorkbookFactory.create(inputStream)
        ) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new RuntimeException(
                        "Excel workbook contains no sheets."
                );
            }

            Sheet sheet =
                    workbook.getSheetAt(0);

            if (sheet == null ||
                    sheet.getPhysicalNumberOfRows() == 0) {

                throw new RuntimeException(
                        "Excel sheet is empty."
                );
            }

            DataFormatter formatter =
                    new DataFormatter();

            // ----------------------------------------------------
            // Find header row.
            //
            // We use the first non-empty row as the header.
            // ----------------------------------------------------

            int headerRowIndex = -1;

            for (
                    int rowIndex = 0;
                    rowIndex <= sheet.getLastRowNum();
                    rowIndex++
            ) {

                Row row =
                        sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                boolean hasValue = false;

                for (
                        int col = 0;
                        col < row.getLastCellNum();
                        col++
                ) {

                    Cell cell =
                            row.getCell(col);

                    String value =
                            getCellValue(
                                    cell,
                                    formatter
                            );

                    if (value != null &&
                            !value.trim().isEmpty()) {

                        hasValue = true;
                        break;
                    }
                }

                if (hasValue) {
                    headerRowIndex = rowIndex;
                    break;
                }
            }

            if (headerRowIndex == -1) {

                throw new RuntimeException(
                        "Could not find Excel header row."
                );
            }

            Row headerRow =
                    sheet.getRow(headerRowIndex);

            int lastColumn =
                    headerRow.getLastCellNum();

            if (lastColumn <= 0) {

                throw new RuntimeException(
                        "Excel header row is empty."
                );
            }

            // ----------------------------------------------------
            // Read column names exactly
            // ----------------------------------------------------

            List<String> headers =
                    new ArrayList<>();

            for (
                    int col = 0;
                    col < lastColumn;
                    col++
            ) {

                Cell cell =
                        headerRow.getCell(col);

                String header =
                        getCellValue(
                                cell,
                                formatter
                        );

                if (header == null) {
                    header = "";
                }

                header = header.trim();

                // Empty Excel column names need a unique name
                if (header.isEmpty()) {

                    header =
                            "Column_" + (col + 1);
                }

                headers.add(header);
            }

            // ----------------------------------------------------
            // Read every Excel row
            // ----------------------------------------------------

            for (
                    int rowIndex = headerRowIndex + 1;
                    rowIndex <= sheet.getLastRowNum();
                    rowIndex++
            ) {

                Row excelRow =
                        sheet.getRow(rowIndex);

                if (excelRow == null) {
                    continue;
                }

                Map<String, Object> rowData =
                        new HashMap<>();

                boolean hasAnyValue = false;

                for (
                        int col = 0;
                        col < headers.size();
                        col++
                ) {

                    Cell cell =
                            excelRow.getCell(col);

                    String value =
                            getCellValue(
                                    cell,
                                    formatter
                            );

                    if (value == null) {
                        value = "";
                    }

                    if (!value.trim().isEmpty()) {
                        hasAnyValue = true;
                    }

                    /*
                     * IMPORTANT:
                     *
                     * We store the ORIGINAL header as the key.
                     * We do not rename the Excel column here.
                     */
                    rowData.put(
                            headers.get(col),
                            value
                    );
                }

                // Completely empty row -> ignore
                if (!hasAnyValue) {
                    continue;
                }

                // ------------------------------------------------
                // Add internal metadata
                //
                // This does NOT replace Excel data.
                // It is used only by the backend to identify
                // the source row.
                // ------------------------------------------------

                rowData.put(
                        "__excelRowNumber",
                        rowIndex + 1
                );

                result.add(rowData);
            }

            return result;
        }
    }

    // ============================================================
    // EXCEL CELL VALUE
    // ============================================================

    private String getCellValue(
            Cell cell,
            DataFormatter formatter) {

        if (cell == null) {
            return "";
        }

        try {

            if (cell.getCellType() ==
                    CellType.FORMULA) {

                return formatter.formatCellValue(cell);
            }

            if (cell.getCellType() ==
                    CellType.NUMERIC) {

                if (DateUtil.isCellDateFormatted(cell)) {

                    return formatter.formatCellValue(cell);
                }
            }

            return formatter.formatCellValue(cell);

        } catch (Exception e) {

            return "";
        }
    }

    // ============================================================
    // DELETE LEAD
    // ============================================================

    @DeleteMapping("/{leadId}")
    public ResponseEntity<Map<String, String>> deleteLead(
            @PathVariable String leadId) {

        leadService.deleteLead(leadId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Lead deleted successfully",
                        "leadId",
                        leadId
                )
        );
    }

    // ============================================================
// DELETE MULTIPLE LEADS
// ============================================================

@DeleteMapping("/batch")
public ResponseEntity<Map<String, Object>> deleteMultipleLeads(
        @RequestBody List<String> leadIds) {

    if (leadIds == null || leadIds.isEmpty()) {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "success", false,
                        "message", "No lead IDs provided"
                )
        );
    }

    int deletedCount = leadService.deleteMultipleLeads(leadIds);

    return ResponseEntity.ok(
            Map.of(
                    "success", true,
                    "message", "Leads deleted successfully",
                    "deletedCount", deletedCount
            )
    );
}

    // ============================================================
    // DELETE ALL
    // ============================================================

    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllLeads() {

        leadService.deleteAllLeads();

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "All leads deleted successfully"
                )
        );
    }

    // ============================================================
    // SUPER ADMIN - FOLLOW-UPS OF ONE TELECALLER
    // ============================================================

    @GetMapping("/followups/telecaller/{username}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<LeadResponse>> getTelecallerFollowUps(
            @PathVariable String username) {

        return ResponseEntity.ok(
                leadService.getFollowUpsForTelecaller(username)
        );
    }

    @GetMapping("/{leadId}/history")
public ResponseEntity<List<LeadActivity>> getLeadHistory(
        @PathVariable String leadId) {
    return ResponseEntity.ok(leadService.getLeadHistory(leadId));
}
}