// FILE: src/main/java/com/indocrm/indocrm/dto/CallResponse.java

package com.indocrm.indocrm.dto;

import java.util.Date;
import java.util.List;

public class CallResponse {
    private String id;
    private String leadId;
    private String leadName;
    private String leadMobile;
    private String contactPerson;
    private String category;
    private String calledNumber;
    private String callerNumber; // NEW: Telecaller's phone number
    private String callMethod;
    private String callStatus;
    private String outcome;
    private Integer duration;
    private String notes;
    private String remarks;
    private String sentiment;
    private List<String> interestedIn;
    private List<ContactNumberDTO> contactNumbers;
    private List<CallAttemptDTO> attempts;
    private String agentUsername;
    private String agentName;
    private Date startedAt;
    private Date endedAt;
    private Date createdAt;
    private Date updatedAt;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    
    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }
    
    public String getLeadMobile() { return leadMobile; }
    public void setLeadMobile(String leadMobile) { this.leadMobile = leadMobile; }
    
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getCalledNumber() { return calledNumber; }
    public void setCalledNumber(String calledNumber) { this.calledNumber = calledNumber; }
    
    public String getCallerNumber() { return callerNumber; }
    public void setCallerNumber(String callerNumber) { this.callerNumber = callerNumber; }
    
    public String getCallMethod() { return callMethod; }
    public void setCallMethod(String callMethod) { this.callMethod = callMethod; }
    
    public String getCallStatus() { return callStatus; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }
    
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    
    public List<String> getInterestedIn() { return interestedIn; }
    public void setInterestedIn(List<String> interestedIn) { this.interestedIn = interestedIn; }
    
    public List<ContactNumberDTO> getContactNumbers() { return contactNumbers; }
    public void setContactNumbers(List<ContactNumberDTO> contactNumbers) { this.contactNumbers = contactNumbers; }
    
    public List<CallAttemptDTO> getAttempts() { return attempts; }
    public void setAttempts(List<CallAttemptDTO> attempts) { this.attempts = attempts; }
    
    public String getAgentUsername() { return agentUsername; }
    public void setAgentUsername(String agentUsername) { this.agentUsername = agentUsername; }
    
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    
    public Date getEndedAt() { return endedAt; }
    public void setEndedAt(Date endedAt) { this.endedAt = endedAt; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
