package com.indocrm.indocrm.dto;

import java.util.Date;
import java.util.List;

public class LeadResponse {

    private String id;
    private String leadId;
    private String name;
    private String mobile;
    private String phoneNumber;
    private String email;
    private String category;
    private String campaign;
    private String status;
    private String assignedTo;
    private String source;
    private String lastContact;
    private String notes;
    private String district;
    private String area;
    private String uploadBatch;
    private String assignedDate;

    private Integer slNo;
    private String school;
    private String schoolAddress;
    private String schoolCurrentLocation;
    private String noNumber;
    private String notExistNumber;
    private String notAnswering;
    private String totalNumberOfAnswering;
    private String afternoonCallFollowup;
    private String conveyForHeadOffice;
    private String alreadySoftwareUsers;
    private String shareProfile;
    private String tcNextRefollowupDate;
    private String meNextVisitDate;
    private String fiftyPercentSubscription;
    private String newSchoolFound;
    private String demoScheduling;
    private String acceptedSubscription;
    private String rejected;
    private String schoolClosed;
    private String numberOfNoNeed;
    private String cmoVisitWithMarketingExecutive;
    private String nextAcademyYearFollowup;
    private String icInstalled;
    private String appointmentSecured;
    private String directVisit;
    private String up1;
    private String up2;
    private String up3;
    private String up4;
    private String up5;

    private Date createdAt;
    private Date updatedAt;

    private String telecallerName;
    private String marketingExecutiveName;
    private String remarks;

    private List<String> updates;
    private java.util.Map<String, Object> excelData;
    private java.util.List<com.indocrm.indocrm.model.LeadActivity> activityHistory;

    // ============== GETTERS & SETTERS ==============

    

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCampaign() { return campaign; }
    public void setCampaign(String campaign) { this.campaign = campaign; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getUpdates() { return updates; }
    public void setUpdates(List<String> updates) { this.updates = updates; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getLastContact() { return lastContact; }
    public void setLastContact(String lastContact) { this.lastContact = lastContact; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getUploadBatch() { return uploadBatch; }
    public void setUploadBatch(String uploadBatch) { this.uploadBatch = uploadBatch; }

    public String getAssignedDate() { return assignedDate; }
    public void setAssignedDate(String assignedDate) { this.assignedDate = assignedDate; }

    public Integer getSlNo() { return slNo; }
    public void setSlNo(Integer slNo) { this.slNo = slNo; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getSchoolAddress() { return schoolAddress; }
    public void setSchoolAddress(String schoolAddress) { this.schoolAddress = schoolAddress; }

    public String getSchoolCurrentLocation() { return schoolCurrentLocation; }
    public void setSchoolCurrentLocation(String schoolCurrentLocation) { this.schoolCurrentLocation = schoolCurrentLocation; }

    public String getNoNumber() { return noNumber; }
    public void setNoNumber(String noNumber) { this.noNumber = noNumber; }

    public String getNotExistNumber() { return notExistNumber; }
    public void setNotExistNumber(String notExistNumber) { this.notExistNumber = notExistNumber; }

    public String getNotAnswering() { return notAnswering; }
    public void setNotAnswering(String notAnswering) { this.notAnswering = notAnswering; }

    public String getTotalNumberOfAnswering() { return totalNumberOfAnswering; }
    public void setTotalNumberOfAnswering(String totalNumberOfAnswering) { this.totalNumberOfAnswering = totalNumberOfAnswering; }

    public String getAfternoonCallFollowup() { return afternoonCallFollowup; }
    public void setAfternoonCallFollowup(String afternoonCallFollowup) { this.afternoonCallFollowup = afternoonCallFollowup; }

    public String getConveyForHeadOffice() { return conveyForHeadOffice; }
    public void setConveyForHeadOffice(String conveyForHeadOffice) { this.conveyForHeadOffice = conveyForHeadOffice; }

    public String getAlreadySoftwareUsers() { return alreadySoftwareUsers; }
    public void setAlreadySoftwareUsers(String alreadySoftwareUsers) { this.alreadySoftwareUsers = alreadySoftwareUsers; }

    public String getShareProfile() { return shareProfile; }
    public void setShareProfile(String shareProfile) { this.shareProfile = shareProfile; }

    public String getTcNextRefollowupDate() { return tcNextRefollowupDate; }
    public void setTcNextRefollowupDate(String tcNextRefollowupDate) { this.tcNextRefollowupDate = tcNextRefollowupDate; }

    public String getMeNextVisitDate() { return meNextVisitDate; }
    public void setMeNextVisitDate(String meNextVisitDate) { this.meNextVisitDate = meNextVisitDate; }

    public String getFiftyPercentSubscription() { return fiftyPercentSubscription; }
    public void setFiftyPercentSubscription(String fiftyPercentSubscription) { this.fiftyPercentSubscription = fiftyPercentSubscription; }

    public String getNewSchoolFound() { return newSchoolFound; }
    public void setNewSchoolFound(String newSchoolFound) { this.newSchoolFound = newSchoolFound; }

    public String getDemoScheduling() { return demoScheduling; }
    public void setDemoScheduling(String demoScheduling) { this.demoScheduling = demoScheduling; }

    public String getAcceptedSubscription() { return acceptedSubscription; }
    public void setAcceptedSubscription(String acceptedSubscription) { this.acceptedSubscription = acceptedSubscription; }

    public String getRejected() { return rejected; }
    public void setRejected(String rejected) { this.rejected = rejected; }

    public String getSchoolClosed() { return schoolClosed; }
    public void setSchoolClosed(String schoolClosed) { this.schoolClosed = schoolClosed; }

    public String getNumberOfNoNeed() { return numberOfNoNeed; }
    public void setNumberOfNoNeed(String numberOfNoNeed) { this.numberOfNoNeed = numberOfNoNeed; }

    public String getCmoVisitWithMarketingExecutive() { return cmoVisitWithMarketingExecutive; }
    public void setCmoVisitWithMarketingExecutive(String cmoVisitWithMarketingExecutive) { this.cmoVisitWithMarketingExecutive = cmoVisitWithMarketingExecutive; }

    public String getNextAcademyYearFollowup() { return nextAcademyYearFollowup; }
    public void setNextAcademyYearFollowup(String nextAcademyYearFollowup) { this.nextAcademyYearFollowup = nextAcademyYearFollowup; }

    public String getIcInstalled() { return icInstalled; }
    public void setIcInstalled(String icInstalled) { this.icInstalled = icInstalled; }

    public String getAppointmentSecured() { return appointmentSecured; }
    public void setAppointmentSecured(String appointmentSecured) { this.appointmentSecured = appointmentSecured; }

    public String getDirectVisit() { return directVisit; }
    public void setDirectVisit(String directVisit) { this.directVisit = directVisit; }

    public String getUp1() { return up1; }
    public void setUp1(String up1) { this.up1 = up1; }

    public String getUp2() { return up2; }
    public void setUp2(String up2) { this.up2 = up2; }

    public String getUp3() { return up3; }
    public void setUp3(String up3) { this.up3 = up3; }

    public String getUp4() { return up4; }
    public void setUp4(String up4) { this.up4 = up4; }

    public String getUp5() { return up5; }
    public void setUp5(String up5) { this.up5 = up5; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getTelecallerName() { return telecallerName; }
    public void setTelecallerName(String telecallerName) { this.telecallerName = telecallerName; }

    public String getMarketingExecutiveName() { return marketingExecutiveName; }
    public void setMarketingExecutiveName(String marketingExecutiveName) { this.marketingExecutiveName = marketingExecutiveName; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public java.util.Map<String, Object> getExcelData() { return excelData; }
    public void setExcelData(java.util.Map<String, Object> excelData) { this.excelData = excelData; }
    public java.util.List<com.indocrm.indocrm.model.LeadActivity> getActivityHistory() { return activityHistory; }
    public void setActivityHistory(java.util.List<com.indocrm.indocrm.model.LeadActivity> activityHistory) { this.activityHistory = activityHistory; }

}
