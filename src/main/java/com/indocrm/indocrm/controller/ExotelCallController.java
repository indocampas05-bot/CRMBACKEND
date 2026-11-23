// FILE: src/main/java/com/indocrm/indocrm/controller/ExotelCallController.java
//
// Two endpoints:
//   1) POST /api/exotel/call             <- Frontend calls this when telecaller clicks "Call"
//   2) POST /api/exotel/webhook/status   <- Exotel calls this (public URL) as the call progresses
//
// NOTE: /api/exotel/webhook/status must be reachable by Exotel's servers - add it to your
// Spring Security "permitAll" list (same place "/bridge/**" was permitted before), since
// Exotel won't send your JWT.

package com.indocrm.indocrm.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.indocrm.indocrm.dto.CallRequest;
import com.indocrm.indocrm.dto.CallResponse;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.TelecallerRepository;
import com.indocrm.indocrm.service.CallService;
import com.indocrm.indocrm.service.ExotelService;
import com.indocrm.indocrm.service.TelecallerAccessService;

@RestController
@RequestMapping("/api/exotel")
public class ExotelCallController {

    @Autowired private ExotelService exotelService;
    @Autowired private CallService callService;
    @Autowired private TelecallerRepository telecallerRepository;
    @Autowired private TelecallerAccessService accessService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    public static class ClickToCallRequest {
        public String leadId;
        public String leadName;
        public String leadMobile;
        public String agentUsername;
    }

    /**
     * Telecaller clicks "Call" on a lead -> frontend POSTs here instead of the old
     * STOMP "/app/bridge/call" message.
     */
    @PostMapping("/call")
    public Map<String, Object> makeCall(@RequestBody ClickToCallRequest req) {
        if (accessService.isTelecaller()) {
            accessService.requireLeadAccess(req.leadId);
        }

        Telecaller agent = telecallerRepository.findByUsername(req.agentUsername)
                .orElseThrow(() -> new RuntimeException("Telecaller not found: " + req.agentUsername));

        if (agent.getMobile() == null || agent.getMobile().isBlank()) {
            throw new RuntimeException("Telecaller has no mobile number saved - add it in Telecaller profile first");
        }

        // 1. Create the Call record up-front in "ringing" state, same as the old flow did
        CallRequest callReq = new CallRequest();
        callReq.setLeadId(req.leadId);
        callReq.setLeadName(req.leadName);
        callReq.setLeadMobile(req.leadMobile);
        callReq.setCalledNumber(req.leadMobile);
        callReq.setCallerNumber(agent.getMobile());
        callReq.setCallMethod("exotel_click_to_call");
        callReq.setCallStatus("ringing");
        callReq.setAgentUsername(agent.getUsername());
        callReq.setAgentName(agent.getFullName());
        CallResponse savedCall = callService.createCall(callReq);

        // 2. Ask Exotel to ring the agent's phone, then bridge to the customer
        String exotelCallSid = exotelService.initiateClickToCall(agent.getMobile(), req.leadMobile, savedCall.getId());

        // 3. Tell the frontend (same topic it already listens to from the old bridge flow)
        Map<String, Object> status = new HashMap<>();
        status.put("callId", savedCall.getId());
        status.put("exotelCallSid", exotelCallSid);
        status.put("leadId", req.leadId);
        status.put("leadName", req.leadName);
        status.put("phoneNumber", req.leadMobile);
        status.put("status", "initiating");
        status.put("callMethod", "exotel_click_to_call");
        status.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/call-status", status);

        Map<String, Object> result = new HashMap<>();
        result.put("callId", savedCall.getId());
        result.put("exotelCallSid", exotelCallSid);
        result.put("message", "Ringing your phone first, then will connect to customer");
        return result;
    }

    /**
     * Exotel posts here as the call progresses. Public endpoint - no JWT, Exotel can't send one.
     * Typical fields Exotel sends: CallSid, Status (e.g. completed/busy/failed/no-answer),
     * DialCallDuration, RecordingUrl, From, To.
     */
    @PostMapping("/webhook/status")
    public String handleStatusCallback(@RequestParam Map<String, String> payload,
                                        @RequestParam(required = false) String leadId) {
        String exotelStatus = payload.getOrDefault("Status", payload.get("DialCallStatus"));
        String durationStr = payload.getOrDefault("DialCallDuration", payload.get("Duration"));
        String callSid = payload.get("CallSid");
        String recordingUrl = payload.get("RecordingUrl");

        // Map Exotel's status vocabulary to whatever your Call.callStatus already uses
        String mappedStatus = mapStatus(exotelStatus);

        Map<String, Object> status = new HashMap<>();
        status.put("exotelCallSid", callSid);
        status.put("leadId", leadId);
        status.put("status", mappedStatus);
        status.put("duration", durationStr);
        status.put("recordingUrl", recordingUrl);
        status.put("callMethod", "exotel_click_to_call");
        status.put("timestamp", System.currentTimeMillis());

        // Push live update to whoever is watching this call in the CRM UI
        messagingTemplate.convertAndSend("/topic/call-status", status);

        // TODO: also persist final duration/outcome onto the saved Call record here via
        // callService.updateCall(...) once you decide how you want to look the Call back up
        // (e.g. store exotelCallSid on the Call model when you create it, then query by that).

        return "OK"; // Exotel expects a 200 response, body content doesn't matter
    }

    private String mapStatus(String exotelStatus) {
        if (exotelStatus == null) return "unknown";
        switch (exotelStatus.toLowerCase()) {
            case "completed": return "completed";
            case "busy": return "busy";
            case "no-answer":
            case "noanswer": return "no_answer";
            case "failed": return "failed";
            case "in-progress":
            case "in progress": return "in_progress";
            default: return exotelStatus;
        }
    }
}
