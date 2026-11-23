// BACKEND: src/main/java/com/indocrm/indocrm/controller/CallController.java

package com.indocrm.indocrm.controller;

import com.indocrm.indocrm.dto.CallRequest;
import com.indocrm.indocrm.dto.CallResponse;
import com.indocrm.indocrm.dto.CallStatsResponse;
import com.indocrm.indocrm.service.CallService;
import com.indocrm.indocrm.service.TelecallerAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calls")
@CrossOrigin(origins = {
    "http://localhost:5173",
    "http://localhost:5174",
    "http://127.0.0.1:5173",
    "http://127.0.0.1:5174",
    "http://192.168.0.107:5173",
    "http://192.168.0.107:5174",
    "https://*.trycloudflare.com"
}, allowCredentials = "true")
public class CallController {

    private final CallService callService;
    private final TelecallerAccessService accessService;

    public CallController(CallService callService, TelecallerAccessService accessService) {
        this.callService = callService;
        this.accessService = accessService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<CallResponse> createCall(@RequestBody CallRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (accessService.isTelecaller()) accessService.requireLeadAccess(request.getLeadId());
        request.setAgentUsername(username);
        
        CallResponse response = callService.createCall(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<CallResponse> updateCall(@PathVariable String id, @RequestBody CallRequest request) {
        if (accessService.isTelecaller()) {
            CallResponse existing = callService.getCallById(id);
            accessService.requireLeadAccess(existing.getLeadId());
        }
        CallResponse response = callService.updateCall(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<CallResponse> updateCallStatus(@PathVariable String id, @RequestBody Map<String, String> payload) {
        if (accessService.isTelecaller()) { accessService.requireLeadAccess(callService.getCallById(id).getLeadId()); }
        String status = payload.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        CallRequest request = new CallRequest();
        request.setCallStatus(status);
        CallResponse response = callService.updateCall(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/outcome")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<CallResponse> updateCallOutcome(@PathVariable String id, @RequestBody Map<String, String> payload) {
        if (accessService.isTelecaller()) { accessService.requireLeadAccess(callService.getCallById(id).getLeadId()); }
        String outcome = payload.get("outcome");
        if (outcome == null || outcome.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        CallRequest request = new CallRequest();
        request.setOutcome(outcome);
        CallResponse response = callService.updateCall(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<CallResponse> getCall(@PathVariable String id) {
        if (accessService.isTelecaller()) {
            CallResponse existing = callService.getCallById(id);
            accessService.requireLeadAccess(existing.getLeadId());
        }
        CallResponse response = callService.getCallById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<List<CallResponse>> getCallsByLead(@PathVariable String leadId) {
        if (accessService.isTelecaller()) accessService.requireLeadAccess(leadId);
        List<CallResponse> calls = callService.getCallsByLeadId(leadId);
        return ResponseEntity.ok(calls);
    }

    @GetMapping("/agent")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<List<CallResponse>> getMyCalls() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<CallResponse> calls = callService.getCallsByAgent(username);
        return ResponseEntity.ok(calls);
    }

    @GetMapping("/agent/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<CallResponse>> getCallsByAgent(@PathVariable String username) {
        List<CallResponse> calls = callService.getCallsByAgent(username);
        return ResponseEntity.ok(calls);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'EMPLOYEE', 'TELECALLER')")
    public ResponseEntity<CallStatsResponse> getCallStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        CallStatsResponse stats = callService.getCallStats(username);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<CallStatsResponse> getAllCallStats() {
        CallStatsResponse stats = callService.getCallStats(null);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<CallResponse>> getCallsByDateRange(
            @RequestParam Long start,
            @RequestParam Long end) {
        Date startDate = new Date(start);
        Date endDate = new Date(end);
        List<CallResponse> calls = callService.getCallsByDateRange(startDate, endDate);
        return ResponseEntity.ok(calls);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCall(@PathVariable String id) {
        callService.deleteCall(id);
        return ResponseEntity.ok(Map.of("message", "Call deleted successfully"));
    }

    @DeleteMapping("/lead/{leadId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCallsByLead(@PathVariable String leadId) {
        callService.deleteCallsByLeadId(leadId);
        return ResponseEntity.ok(Map.of("message", "All calls for lead deleted successfully"));
    }
}
