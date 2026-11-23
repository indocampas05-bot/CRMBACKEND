// src/main/java/com/indocrm/indocrm/controller/CallBridgeController.java

package com.indocrm.indocrm.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indocrm.indocrm.dto.BridgeCallRequest;
import com.indocrm.indocrm.dto.BridgeCallStatus;
import com.indocrm.indocrm.service.CallBridgeService;
import com.indocrm.indocrm.service.TelecallerAccessService;

@RestController
@RequestMapping("/api/bridge")
public class CallBridgeController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CallBridgeService callBridgeService;

    @Autowired
    private TelecallerAccessService accessService;

    @Autowired
    private ObjectMapper objectMapper;

    // ============================================================
    // WebRTC Signaling Storage (FREE - In-memory, no TURN needed)
    // ============================================================
    private final Map<String, String> pendingSDP = new ConcurrentHashMap<>();
    private final Map<String, String> pendingICE = new ConcurrentHashMap<>();

    @MessageMapping("/bridge/call")
    public void initiateCall(@Payload Map<String, Object> payload) {
        System.out.println("📨 Received bridge call request: " + payload);
        
        try {
            Map<String, Object> data;
            if (payload.containsKey("body")) {
                String bodyStr = (String) payload.get("body");
                data = objectMapper.readValue(bodyStr, Map.class);
            } else {
                data = payload;
            }
            
            if (data.containsKey("type") && "bridge_call".equals(data.get("type"))) {
                data = (Map<String, Object>) data.get("data");
            }
            
            BridgeCallRequest request = new BridgeCallRequest();
            request.setDeviceId((String) data.get("deviceId"));
            request.setLeadId((String) data.get("leadId"));
            request.setLeadName((String) data.get("leadName"));
            request.setPhoneNumber((String) data.get("phoneNumber"));
            request.setCallerNumber((String) data.get("callerNumber"));
            request.setAgentUsername((String) data.get("agentUsername"));
            request.setAgentName((String) data.get("agentName"));
            request.setCallMethod((String) data.get("callMethod"));
            request.setAudioMode((String) data.get("audioMode"));

            String registeredUsername = callBridgeService.getDeviceUsername(request.getDeviceId());
            if (registeredUsername == null || !registeredUsername.equals(request.getAgentUsername())) {
                throw new AccessDeniedException("Device is not registered for this telecaller");
            }
            if (accessService.isTelecaller()) {
                accessService.requireLeadAccess(request.getLeadId());
            } else {
                com.indocrm.indocrm.model.Telecaller tc = accessService.currentTelecallerByUsername(request.getAgentUsername());
                if (tc.getDistricts() == null || request.getLeadId() == null) {
                    throw new AccessDeniedException("Lead access denied");
                }
                accessService.requireLeadAccessForUsername(request.getAgentUsername(), request.getLeadId());
            }
            request.setTimestamp(System.currentTimeMillis());
            
            System.out.println("📞 Forwarding call to device: " + request.getDeviceId());
            
            messagingTemplate.convertAndSendToUser(
                request.getDeviceId(),
                "/queue/call",
                request
            );
            
            messagingTemplate.convertAndSend("/topic/call-requests", request);
            
            BridgeCallStatus status = new BridgeCallStatus();
            status.setDeviceId(request.getDeviceId());
            status.setLeadId(request.getLeadId());
            status.setLeadName(request.getLeadName());
            status.setPhoneNumber(request.getPhoneNumber());
            status.setStatus("initiating");
            status.setCallMethod("android_sim_pc_headset");
            status.setAudioRoute("webrtc_p2p");
            status.setTimestamp(System.currentTimeMillis());
            
            messagingTemplate.convertAndSend("/topic/call-status", status);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to process bridge call: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @MessageMapping("/bridge/status")
    public void handleBridgeStatus(@Payload BridgeCallStatus status) {
        System.out.println("📊 Received bridge status: " + status.getStatus());
        System.out.println("📊 Device: " + status.getDeviceId());
        System.out.println("📊 Lead: " + status.getLeadName());
        System.out.println("📊 Phone: " + status.getPhoneNumber());
        System.out.println("📊 Audio Route: " + status.getAudioRoute());
        System.out.println("📊 Call Method: " + status.getCallMethod());
        
        if (status.getAudioRoute() == null) status.setAudioRoute("webrtc_p2p");
        if (status.getCallMethod() == null) status.setCallMethod("android_sim_pc_headset");
        
        callBridgeService.saveCallStatus(status);
        
        messagingTemplate.convertAndSend("/topic/call-status", status);
        messagingTemplate.convertAndSend("/topic/call/" + status.getDeviceId(), status);
    }

    @MessageMapping("/bridge/register")
    public void registerDevice(@Payload Map<String, String> payload) {
        String deviceId = payload.get("deviceId");
        String username = payload.get("username");
        String deviceType = payload.get("deviceType");
        
        System.out.println("📱 Device registered: " + deviceId + " by " + username + " (type: " + deviceType + ")");
        callBridgeService.registerDevice(deviceId, username, deviceType);
        
        messagingTemplate.convertAndSendToUser(
            deviceId,
            "/queue/register",
            Map.of("status", "success", "deviceId", deviceId, "deviceType", deviceType)
        );
    }

    // ============================================================
    // FREE WebRTC Signaling Endpoints - No TURN server needed!
    // ============================================================

    @MessageMapping("/webrtc/signal")
    public void handleWebRTCSignal(@Payload Map<String, Object> payload) {
        System.out.println("🎧 WebRTC signal: " + payload);

        String deviceId = (String) payload.get("deviceId");
        String type = (String) payload.get("type");

        if (deviceId == null) {
            System.err.println("❌ No deviceId in WebRTC signal");
            return;
        }

        if ("sdp".equals(type)) {
            String sdp = (String) payload.get("sdp");
            System.out.println("🎧 SDP from " + deviceId + ": " + sdp.substring(0, Math.min(100, sdp.length())));

            pendingSDP.put(deviceId, sdp);

            // Forward to web UI if it's from Android
            messagingTemplate.convertAndSend("/topic/webrtc/sdp", Map.of(
                "deviceId", deviceId,
                "sdp", sdp,
                "type", "sdp"
            ));
            System.out.println("📤 SDP forwarded to web");
        }

        if ("ice".equals(type)) {
            String ice = (String) payload.get("ice");
            System.out.println("🎧 ICE from " + deviceId + ": " + ice);

            pendingICE.put(deviceId, ice);

            // Forward to web UI if it's from Android
            messagingTemplate.convertAndSend("/topic/webrtc/ice", Map.of(
                "deviceId", deviceId,
                "ice", ice,
                "type", "ice"
            ));
            System.out.println("📤 ICE forwarded to web");
        }
    }

    // WebRTC endpoints for web to fetch SDP/ICE from Android
    @GetMapping("/webrtc/sdp/{deviceId}")
    public Map<String, String> getSDPForDevice(@PathVariable String deviceId) {
        String sdp = pendingSDP.get(deviceId);
        return Map.of("sdp", sdp != null ? sdp : "");
    }

    @GetMapping("/webrtc/ice/{deviceId}")
    public Map<String, String> getICEForDevice(@PathVariable String deviceId) {
        String ice = pendingICE.get(deviceId);
        return Map.of("ice", ice != null ? ice : "");
    }

    @GetMapping("/devices")
    public Map<String, Object> getActiveDevices() {
        return callBridgeService.getActiveDevices();
    }
}