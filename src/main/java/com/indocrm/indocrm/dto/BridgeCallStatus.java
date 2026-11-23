// src/main/java/com/indocrm/indocrm/dto/BridgeCallStatus.java

package com.indocrm.indocrm.dto;

public class BridgeCallStatus {
    private String callId;
    private String deviceId;
    private String leadId;
    private String leadName;
    private String phoneNumber;
    private String callerNumber;
    private String status; // initiating, ringing, connected, ended, missed, failed, busy, rejected
    private String outcome;
    private Integer duration;
    private String notes;
    private String agentUsername;
    private String agentName;
    private Long timestamp;
    private Integer attemptNumber;
    private String audioRoute;
    private String callMethod;

    // Getters and Setters
    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    
    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getCallerNumber() { return callerNumber; }
    public void setCallerNumber(String callerNumber) { this.callerNumber = callerNumber; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getAgentUsername() { return agentUsername; }
    public void setAgentUsername(String agentUsername) { this.agentUsername = agentUsername; }
    
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    
    public String getAudioRoute() { return audioRoute; }
    public void setAudioRoute(String audioRoute) { this.audioRoute = audioRoute; }
    
    public String getCallMethod() { return callMethod; }
    public void setCallMethod(String callMethod) { this.callMethod = callMethod; }
}