// FILE: src/main/java/com/indocrm/indocrm/dto/LeadImportDTO.java

package com.indocrm.indocrm.dto;

public class LeadImportDTO {
    private String schoolName;
    private String category;
    private String address;
    private String block;
    private String udiseCode;
    private String phoneNumber;
    private String googleMapLink;
    private String district;
    private String clientName;
    private String notes;
    
    public LeadImportDTO() {}

    // Getters and Setters
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }
    
    public String getUdiseCode() { return udiseCode; }
    public void setUdiseCode(String udiseCode) { this.udiseCode = udiseCode; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getGoogleMapLink() { return googleMapLink; }
    public void setGoogleMapLink(String googleMapLink) { this.googleMapLink = googleMapLink; }
    
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
