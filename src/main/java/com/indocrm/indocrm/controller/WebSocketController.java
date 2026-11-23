// FILE: src/main/java/com/indocrm/indocrm/controller/WebSocketController.java

package com.indocrm.indocrm.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import com.indocrm.indocrm.service.TelecallerAccessService;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, String> activeDevices = new ConcurrentHashMap<>();

    @Autowired
    private TelecallerAccessService accessService;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/register-device")
    public void registerDevice(@Payload Map<String, String> payload) {
        String deviceId = payload.get("deviceId");
        String username = payload.get("username");
        activeDevices.put(deviceId, username);
        messagingTemplate.convertAndSend("/topic/devices", 
            Map.of("type", "device_registered", "deviceId", deviceId, "username", username));
    }

    @MessageMapping("/make-call")
    public void makeCall(@Payload Map<String, Object> payload) {
        String deviceId = (String) payload.get("deviceId");
        String number = (String) payload.get("number");
        String leadId = (String) payload.get("leadId");
        String registeredUsername = activeDevices.get(deviceId);
        if (registeredUsername == null) throw new AccessDeniedException("Device is not registered");
        accessService.requireLeadAccessForUsername(registeredUsername, leadId);
        
        messagingTemplate.convertAndSendToUser(deviceId, "/queue/calls", 
            Map.of("type", "incoming_call", "number", number, "leadId", leadId));
        
        messagingTemplate.convertAndSend("/topic/call-status", 
            Map.of("type", "call_initiated", "leadId", leadId, "status", "ringing"));
    }

    @MessageMapping("/call-status")
    public void callStatus(@Payload Map<String, Object> payload) {
        String leadId = (String) payload.get("leadId");
        String status = (String) payload.get("status");
        
        messagingTemplate.convertAndSend("/topic/call-status", 
            Map.of("type", "call_status_update", "leadId", leadId, "status", status));
    }

    @MessageMapping("/end-call")
    public void endCall(@Payload Map<String, Object> payload) {
        String deviceId = (String) payload.get("deviceId");
        String leadId = (String) payload.get("leadId");
        String registeredUsername = activeDevices.get(deviceId);
        if (registeredUsername == null) throw new AccessDeniedException("Device is not registered");
        accessService.requireLeadAccessForUsername(registeredUsername, leadId);
        
        messagingTemplate.convertAndSendToUser(deviceId, "/queue/calls", 
            Map.of("type", "end_call", "leadId", leadId));
        
        messagingTemplate.convertAndSend("/topic/call-status", 
            Map.of("type", "call_ended", "leadId", leadId, "status", "ended"));
    }
}
