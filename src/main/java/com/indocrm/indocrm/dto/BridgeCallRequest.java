// src/main/java/com/indocrm/indocrm/dto/BridgeCallRequest.java

package com.indocrm.indocrm.dto;

public class BridgeCallRequest {
    private String deviceId;
    private String leadId;
    private String leadName;
    private String phoneNumber;
    private String callerNumber;
    private String agentUsername;
    private String agentName;
    private Long timestamp;
    private String callMethod;
    private String audioMode;

    // Getters and Setters
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
    
    public String getAgentUsername() { return agentUsername; }
    public void setAgentUsername(String agentUsername) { this.agentUsername = agentUsername; }
    
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    
    public String getCallMethod() { return callMethod; }
    public void setCallMethod(String callMethod) { this.callMethod = callMethod; }
    
    public String getAudioMode() { return audioMode; }
    public void setAudioMode(String audioMode) { this.audioMode = audioMode; }
}