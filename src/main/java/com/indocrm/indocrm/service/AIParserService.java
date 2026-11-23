// // E:\indocrm\src\main\java\com\indocrm\indocrm\service\AIParserService.java
// package com.indocrm.indocrm.service;

// import java.util.HashMap;
// import java.util.Map;

// import org.springframework.stereotype.Service;

// @Service
// public class AIParserService {

//     /**
//      * Parse a single row of Excel data - FALLBACK to direct extraction
//      */
//     public ParsedRow parseExcelRow(Map<String, Object> rowData, int rowNumber) {
//         ParsedRow result = new ParsedRow();
        
//         // Extract values directly from the row
//         result.school = getValue(rowData, "school");
//         result.schoolAddress = getValue(rowData, "schoolAddress");
//         result.name = getValue(rowData, "name");
//         result.mobile = getValue(rowData, "mobile");
//         result.telecallingUpdates = getValue(rowData, "telecallingUpdates");
//         result.schoolCurrentLocation = getValue(rowData, "schoolCurrentLocation");
//         result.noNumber = getValue(rowData, "noNumber");
//         result.notExistNumber = getValue(rowData, "notExistNumber");
//         result.notAnswering = getValue(rowData, "notAnswering");
//         result.totalNumberOfAnswering = getValue(rowData, "totalNumberOfAnswering");
//         result.afternoonCallFollowup = getValue(rowData, "afternoonCallFollowup");
//         result.conveyForHeadOffice = getValue(rowData, "conveyForHeadOffice");
//         result.alreadySoftwareUsers = getValue(rowData, "alreadySoftwareUsers");
//         result.shareProfile = getValue(rowData, "shareProfile");
//         result.tcNextRefollowupDate = getValue(rowData, "tcNextRefollowupDate");
//         result.meNextVisitDate = getValue(rowData, "meNextVisitDate");
//         result.fiftyPercentSubscription = getValue(rowData, "fiftyPercentSubscription");
//         result.newSchoolFound = getValue(rowData, "newSchoolFound");
//         result.demoScheduling = getValue(rowData, "demoScheduling");
//         result.acceptedSubscription = getValue(rowData, "acceptedSubscription");
//         result.rejected = getValue(rowData, "rejected");
//         result.schoolClosed = getValue(rowData, "schoolClosed");
//         result.numberOfNoNeed = getValue(rowData, "numberOfNoNeed");
//         result.cmoVisitWithMarketingExecutive = getValue(rowData, "cmoVisitWithMarketingExecutive");
//         result.nextAcademyYearFollowup = getValue(rowData, "nextAcademyYearFollowup");
//         result.icInstalled = getValue(rowData, "icInstalled");
//         result.appointmentSecured = getValue(rowData, "appointmentSecured");
//         result.directVisit = getValue(rowData, "directVisit");
        
//         return result;
//     }

//     private String getValue(Map<String, Object> row, String key) {
//         if (row == null || key == null) return null;
//         for (Map.Entry<String, Object> entry : row.entrySet()) {
//             if (entry.getKey().equalsIgnoreCase(key)) {
//                 return entry.getValue() != null ? entry.getValue().toString().trim() : null;
//             }
//         }
//         return null;
//     }

//     public static class ParsedRow {
//         public String school = "";
//         public String schoolAddress = "";
//         public String name = "";
//         public String mobile = "";
//         public String telecallingUpdates = "";
//         public String schoolCurrentLocation = "";
//         public String noNumber = "";
//         public String notExistNumber = "";
//         public String notAnswering = "";
//         public String totalNumberOfAnswering = "";
//         public String afternoonCallFollowup = "";
//         public String conveyForHeadOffice = "";
//         public String alreadySoftwareUsers = "";
//         public String shareProfile = "";
//         public String tcNextRefollowupDate = "";
//         public String meNextVisitDate = "";
//         public String fiftyPercentSubscription = "";
//         public String newSchoolFound = "";
//         public String demoScheduling = "";
//         public String acceptedSubscription = "";
//         public String rejected = "";
//         public String schoolClosed = "";
//         public String numberOfNoNeed = "";
//         public String cmoVisitWithMarketingExecutive = "";
//         public String nextAcademyYearFollowup = "";
//         public String icInstalled = "";
//         public String appointmentSecured = "";
//         public String directVisit = "";
//         public Map<Integer, UpdateEntry> updates = new HashMap<>();
//     }

//     public static class UpdateEntry {
//         public int number;
//         public String date;
//         public String note;
        
//         public UpdateEntry(int number, String date, String note) {
//             this.number = number;
//             this.date = date;
//             this.note = note;
//         }
//     }
// }
