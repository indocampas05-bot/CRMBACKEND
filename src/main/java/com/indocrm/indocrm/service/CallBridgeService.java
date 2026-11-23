// src/main/java/com/indocrm/indocrm/service/CallBridgeService.java

package com.indocrm.indocrm.service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.dto.BridgeCallStatus;
import com.indocrm.indocrm.model.Call;
import com.indocrm.indocrm.model.CallAttempt;
import com.indocrm.indocrm.repository.CallRepository;
import com.indocrm.indocrm.repository.LeadRepository;

@Service
public class CallBridgeService {

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private LeadRepository leadRepository;

    private final Map<String, Map<String, Object>> activeDevices = new ConcurrentHashMap<>();

    public void saveCallStatus(BridgeCallStatus status) {
        System.out.println("💾 Saving call status: " + status.getStatus());
        System.out.println("💾 Lead: " + status.getLeadName());
        System.out.println("💾 Phone: " + status.getPhoneNumber());
        System.out.println("💾 Duration: " + status.getDuration());
        System.out.println("💾 Audio Route: " + status.getAudioRoute());
        System.out.println("💾 Call Method: " + status.getCallMethod());
        
        // Update lead status when call ends
        if (status.getLeadId() != null && ("ended".equals(status.getStatus()) || "failed".equals(status.getStatus()))) {
            leadRepository.findById(status.getLeadId()).ifPresent(lead -> {
                lead.setLastContact(new Date().toString());
                if (status.getOutcome() != null) {
                    lead.setStatus(status.getOutcome());
                }
                lead.setUpdatedAt(new Date());
                leadRepository.save(lead);
                System.out.println("✅ Lead updated: " + lead.getName() + " -> " + status.getOutcome());
            });
        }

        // Save or update call record
        if (status.getCallId() != null) {
            callRepository.findById(status.getCallId()).ifPresentOrElse(
                call -> {
                    if (status.getDuration() != null && status.getDuration() > 0) {
                        call.setDuration(status.getDuration());
                    }
                    if ("connected".equals(status.getStatus()) && call.getStartedAt() == null) {
                        call.setStartedAt(new Date(status.getTimestamp()));
                    }
                    if ("ended".equals(status.getStatus())) {
                        call.setEndedAt(new Date(status.getTimestamp()));
                        call.setCallStatus("ended");
                    }
                    if (status.getOutcome() != null) {
                        call.setOutcome(status.getOutcome());
                    }
                    if (status.getNotes() != null) {
                        call.setNotes(status.getNotes());
                    }
                    call.setUpdatedAt(new Date());
                    callRepository.save(call);
                    System.out.println("✅ Call updated: " + call.getId());
                },
                () -> {
                    // Create new call
                    Call call = new Call();
                    call.setLeadId(status.getLeadId());
                    call.setLeadName(status.getLeadName());
                    call.setLeadMobile(status.getPhoneNumber());
                    call.setCalledNumber(status.getPhoneNumber());
                    call.setCallerNumber(status.getCallerNumber());
                    call.setCallMethod(status.getCallMethod() != null ? status.getCallMethod() : "android_sim_pc_headset");
                    call.setCallStatus(status.getStatus());
                    call.setOutcome(status.getOutcome());
                    call.setDuration(status.getDuration());
                    call.setNotes(status.getNotes());
                    call.setAgentUsername(status.getAgentUsername());
                    call.setAgentName(status.getAgentName());
                    call.setStartedAt(new Date(status.getTimestamp()));
                    call.setCreatedAt(new Date());
                    call.setUpdatedAt(new Date());
                    
                    if (status.getAttemptNumber() != null) {
                        CallAttempt attempt = new CallAttempt();
                        attempt.setNumber(status.getPhoneNumber());
                        attempt.setStatus(status.getStatus());
                        attempt.setTime(new Date(status.getTimestamp()).toString());
                        attempt.setDuration(status.getDuration());
                        call.setAttempts(Collections.singletonList(attempt));
                    }
                    
                    callRepository.save(call);
                    System.out.println("✅ New call created with method: " + call.getCallMethod());
                }
            );
        }
    }

    public Map<String, Object> registerDevice(String deviceId, String username, String deviceType) {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("username", username);
        deviceInfo.put("deviceType", deviceType != null ? deviceType : "android_bridge");
        deviceInfo.put("registeredAt", new Date());
        deviceInfo.put("active", true);
        deviceInfo.put("audioRoute", "bluetooth_pc");
        
        activeDevices.put(deviceId, deviceInfo);
        System.out.println("✅ Device registered: " + deviceId + " (type: " + deviceType + ")");
        
        return Map.of(
            "success", true,
            "message", "Device registered successfully",
            "deviceId", deviceId,
            "deviceType", deviceType != null ? deviceType : "android_bridge"
        );
    }

    public Map<String, Object> getActiveDevices() {
        return Map.of(
            "devices", activeDevices,
            "count", activeDevices.size()
        );
    }
    
    public String getDeviceUsername(String deviceId) {
        Map<String, Object> info = activeDevices.get(deviceId);
        return info == null ? null : (String) info.get("username");
    }
    
    public String getDeviceType(String deviceId) {
        Map<String, Object> info = activeDevices.get(deviceId);
        return info == null ? null : (String) info.get("deviceType");
    }
    
    public Map<String, Object> getDeviceInfo(String deviceId) {
        return activeDevices.get(deviceId);
    }
}