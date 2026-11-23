// BACKEND: src/main/java/com/indocrm/indocrm/service/CallService.java
// Full working file with all methods

package com.indocrm.indocrm.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.indocrm.indocrm.dto.CallAttemptDTO;
import com.indocrm.indocrm.dto.CallRequest;
import com.indocrm.indocrm.dto.CallResponse;
import com.indocrm.indocrm.dto.CallStatsResponse;
import com.indocrm.indocrm.dto.ContactNumberDTO;
import com.indocrm.indocrm.model.Call;
import com.indocrm.indocrm.model.CallAttempt;
import com.indocrm.indocrm.model.ContactNumber;
import com.indocrm.indocrm.repository.CallRepository;
import com.indocrm.indocrm.repository.LeadRepository;

@Service
public class CallService {

    private final CallRepository callRepository;
    private final LeadRepository leadRepository;
    private final LeadService leadService;

    public CallService(CallRepository callRepository, LeadRepository leadRepository, LeadService leadService) {
        this.callRepository = callRepository;
        this.leadRepository = leadRepository;
        this.leadService = leadService;
    }

    @Transactional
    public CallResponse createCall(CallRequest request) {
        if (request.getLeadId() != null) leadService.assertLeadAccess(leadRepository.findByLeadId(request.getLeadId()).orElseThrow(() -> new RuntimeException("Lead not found")));
        Call call = new Call();
        
        call.setLeadId(request.getLeadId());
        call.setLeadName(request.getLeadName());
        call.setLeadMobile(request.getLeadMobile());
        call.setContactPerson(request.getContactPerson());
        call.setCategory(request.getCategory());
        
        call.setCalledNumber(request.getCalledNumber());
        call.setCallerNumber(request.getCallerNumber());
        call.setCallMethod(request.getCallMethod());
        call.setCallStatus(request.getCallStatus() != null ? request.getCallStatus() : "ringing");
        call.setOutcome(request.getOutcome());
        call.setDuration(request.getDuration());
        
        call.setNotes(request.getNotes());
        call.setRemarks(request.getRemarks());
        call.setSentiment(request.getSentiment());
        call.setInterestedIn(request.getInterestedIn());
        
        if (request.getContactNumbers() != null) {
            List<ContactNumber> contacts = request.getContactNumbers().stream()
                .map(dto -> {
                    ContactNumber cn = new ContactNumber();
                    cn.setId(dto.getId());
                    cn.setNumber(dto.getNumber());
                    cn.setLabel(dto.getLabel());
                    cn.setContactPerson(dto.getContactPerson());
                    cn.setPrimary(dto.isPrimary());
                    return cn;
                })
                .collect(Collectors.toList());
            call.setContactNumbers(contacts);
        }
        
        if (request.getAttempts() != null) {
            List<CallAttempt> attempts = request.getAttempts().stream()
                .map(dto -> new CallAttempt(dto.getNumber(), dto.getStatus(), dto.getTime()))
                .collect(Collectors.toList());
            call.setAttempts(attempts);
        }
        
        call.setAgentUsername(request.getAgentUsername());
        call.setAgentName(request.getAgentName());
        
        Date now = new Date();
        call.setStartedAt(request.getStartedAt() != null ? request.getStartedAt() : now);
        call.setEndedAt(request.getEndedAt());
        call.setCreatedAt(now);
        call.setUpdatedAt(now);
        
        Call saved = callRepository.save(call);
        
        if (request.getOutcome() != null && request.getLeadId() != null) {
            leadRepository.findById(request.getLeadId()).ifPresent(lead -> {
                String oldStatus = lead.getStatus();
                String oldLeadStatus = lead.getStatus();
                lead.setStatus(request.getOutcome());
                lead.setUpdatedAt(new Date());
                leadRepository.save(lead);
                leadService.recordActivity(lead, "CALL", "Call Panel", "Call updated", "status", oldLeadStatus, request.getOutcome(),
                        "Call outcome updated", call.getAgentUsername());
                leadRepository.save(lead);
                leadService.recordActivity(lead, "CALL", "Call Panel", "Call outcome", "status", oldStatus, request.getOutcome(),
                        "Call created: " + (request.getCallStatus() == null ? "ringing" : request.getCallStatus()), request.getAgentUsername());
                leadRepository.save(lead);
            });
        } else if (request.getLeadId() != null) {
            leadService.recordActivity(request.getLeadId(), "CALL", "Call Panel", "Call Made", null, null, null,
                    "Call initiated to " + request.getCalledNumber(), request.getAgentUsername());
        }
        
        return convertToResponse(saved);
    }

    @Transactional
    public CallResponse updateCall(String id, CallRequest request) {
        Call call = callRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Call not found: " + id));
        if (call.getLeadId() != null) leadService.assertLeadAccess(leadRepository.findByLeadId(call.getLeadId()).orElseThrow(() -> new RuntimeException("Lead not found")));
        String oldOutcome = call.getOutcome();
        String oldStatus = call.getCallStatus();
        
        if (request.getCallStatus() != null) {
            call.setCallStatus(request.getCallStatus());
        }
        if (request.getOutcome() != null) {
            call.setOutcome(request.getOutcome());
        }
        if (request.getDuration() != null) {
            call.setDuration(request.getDuration());
        }
        if (request.getNotes() != null) {
            call.setNotes(request.getNotes());
        }
        if (request.getRemarks() != null) {
            call.setRemarks(request.getRemarks());
        }
        if (request.getSentiment() != null) {
            call.setSentiment(request.getSentiment());
        }
        if (request.getInterestedIn() != null) {
            call.setInterestedIn(request.getInterestedIn());
        }
        
        if (request.getAttempts() != null) {
            List<CallAttempt> attempts = request.getAttempts().stream()
                .map(dto -> new CallAttempt(dto.getNumber(), dto.getStatus(), dto.getTime()))
                .collect(Collectors.toList());
            call.setAttempts(attempts);
        }
        
        if (request.getStartedAt() != null && call.getStartedAt() == null) {
            call.setStartedAt(request.getStartedAt());
        }
        if (request.getEndedAt() != null) {
            call.setEndedAt(request.getEndedAt());
        } else if (request.getCallStatus() != null
                && ("ended".equalsIgnoreCase(request.getCallStatus())
                    || "completed".equalsIgnoreCase(request.getCallStatus()))) {
            if (call.getEndedAt() == null) call.setEndedAt(new Date());
        }
        
        call.setUpdatedAt(new Date());
        
        Call updated = callRepository.save(call);
        
        if (request.getOutcome() != null && 
            ("ended".equals(request.getCallStatus()) || "connected".equals(request.getCallStatus())) &&
            call.getLeadId() != null) {
            leadRepository.findById(call.getLeadId()).ifPresent(lead -> {
                lead.setStatus(request.getOutcome());
                lead.setUpdatedAt(new Date());
                leadRepository.save(lead);
            });
        }
        
        return convertToResponse(updated);
    }

    public CallResponse getCallById(String id) {
        Call call = callRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Call not found: " + id));
        return convertToResponse(call);
    }

    public List<CallResponse> getCallsByLeadId(String leadId) {
        leadService.assertLeadAccess(leadRepository.findById(leadId).orElseThrow(() -> new RuntimeException("Lead not found")));
        return callRepository.findByLeadId(leadId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CallResponse> getCallsByAgent(String username) {
        return callRepository.findByAgentUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ✅ FIXED: Added this missing method
    public List<CallResponse> getCallsByDateRange(Date start, Date end) {
        List<Call> calls = callRepository.findByStartedAtBetween(start, end);
        return calls.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CallResponse> getCallsByAgentAndDateRange(String username, Date start, Date end) {
        List<Call> calls = callRepository.findByAgentUsernameAndStartedAtBetween(username, start, end);
        return calls.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public CallStatsResponse getCallStats(String agentUsername) {
        List<Call> calls;
        if (agentUsername != null && !agentUsername.isEmpty()) {
            calls = callRepository.findByAgentUsername(agentUsername);
        } else {
            calls = callRepository.findAll();
        }
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();
        Date todayEnd = new Date();
        
        List<Call> todayCalls = calls.stream()
                .filter(c -> c.getStartedAt() != null && 
                            c.getStartedAt().after(todayStart) && 
                            c.getStartedAt().before(todayEnd))
                .collect(Collectors.toList());
        
        CallStatsResponse stats = new CallStatsResponse();
        stats.setTotalCalls(todayCalls.size());
        
        Map<String, Integer> outcomeCounts = new HashMap<>();
        Map<String, Integer> statusCounts = new HashMap<>();
        
        int answered = 0;
        int notAnswered = 0;
        int interested = 0;
        int notInterested = 0;
        int callBack = 0;
        int talkSecs = 0;
        
        for (Call call : todayCalls) {
            String status = call.getCallStatus();
            statusCounts.merge(status != null ? status : "unknown", 1, Integer::sum);
            
            String outcome = call.getOutcome();
            if (outcome != null) {
                outcomeCounts.merge(outcome, 1, Integer::sum);
                
                if ("Interested".equals(outcome)) {
                    interested++;
                } else if ("Not Interested".equals(outcome)) {
                    notInterested++;
                } else if ("Call Back".equals(outcome)) {
                    callBack++;
                }
            }
            
            if ("connected".equals(status) || "ended".equals(status)) {
                answered++;
                if (call.getDuration() != null) {
                    talkSecs += call.getDuration();
                }
            } else {
                notAnswered++;
            }
        }
        
        stats.setAnswered(answered);
        stats.setNotAnswered(notAnswered);
        stats.setInterested(interested);
        stats.setNotInterested(notInterested);
        stats.setCallBack(callBack);
        stats.setTalkSecs(talkSecs);
        stats.setStatusCounts(statusCounts);
        stats.setOutcomeCounts(outcomeCounts);
        
        return stats;
    }

    public void deleteCall(String id) {
        callRepository.deleteById(id);
    }

    public void deleteCallsByLeadId(String leadId) {
        List<Call> calls = callRepository.findByLeadId(leadId);
        callRepository.deleteAll(calls);
    }

    private CallResponse convertToResponse(Call call) {
        CallResponse response = new CallResponse();
        response.setId(call.getId());
        response.setLeadId(call.getLeadId());
        response.setLeadName(call.getLeadName());
        response.setLeadMobile(call.getLeadMobile());
        response.setContactPerson(call.getContactPerson());
        response.setCategory(call.getCategory());
        response.setCalledNumber(call.getCalledNumber());
        response.setCallerNumber(call.getCallerNumber());
        response.setCallMethod(call.getCallMethod());
        response.setCallStatus(call.getCallStatus());
        response.setOutcome(call.getOutcome());
        response.setDuration(call.getDuration());
        response.setNotes(call.getNotes());
        response.setRemarks(call.getRemarks());
        response.setSentiment(call.getSentiment());
        response.setInterestedIn(call.getInterestedIn());
        response.setAgentUsername(call.getAgentUsername());
        response.setAgentName(call.getAgentName());
        response.setStartedAt(call.getStartedAt());
        response.setEndedAt(call.getEndedAt());
        response.setCreatedAt(call.getCreatedAt());
        response.setUpdatedAt(call.getUpdatedAt());
        
        if (call.getContactNumbers() != null) {
            List<ContactNumberDTO> contactDTOs = call.getContactNumbers().stream()
                .map(cn -> {
                    ContactNumberDTO dto = new ContactNumberDTO();
                    dto.setId(cn.getId());
                    dto.setNumber(cn.getNumber());
                    dto.setLabel(cn.getLabel());
                    dto.setContactPerson(cn.getContactPerson());
                    dto.setPrimary(cn.isPrimary());
                    return dto;
                })
                .collect(Collectors.toList());
            response.setContactNumbers(contactDTOs);
        }
        
        if (call.getAttempts() != null) {
            List<CallAttemptDTO> attemptDTOs = call.getAttempts().stream()
                .map(a -> {
                    CallAttemptDTO dto = new CallAttemptDTO();
                    dto.setNumber(a.getNumber());
                    dto.setStatus(a.getStatus());
                    dto.setTime(a.getTime());
                    dto.setDuration(a.getDuration());
                    return dto;
                })
                .collect(Collectors.toList());
            response.setAttempts(attemptDTOs);
        }
        
        return response;
    }
}
