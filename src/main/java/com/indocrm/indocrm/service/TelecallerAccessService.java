package com.indocrm.indocrm.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.LeadRepository;
import com.indocrm.indocrm.repository.TelecallerRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class TelecallerAccessService {
    private final TelecallerRepository telecallerRepository;
    private final LeadRepository leadRepository;

    // Cache per HTTP request — survives for the whole request lifetime
    private static final String CACHE_KEY = "currentTelecallerCache";

    public TelecallerAccessService(TelecallerRepository t, LeadRepository l) {
        this.telecallerRepository = t;
        this.leadRepository = l;
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) { return null; }
    }

    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.isAuthenticated())
            throw new AccessDeniedException("Authentication required");
        return auth.getName();
    }

    public Telecaller currentTelecaller() {
        HttpServletRequest req = currentRequest();
        if (req != null) {
            Object cached = req.getAttribute(CACHE_KEY);
            if (cached instanceof Telecaller) return (Telecaller) cached;
        }
        String username = currentUsername();
        Telecaller tc = telecallerRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Telecaller account not found"));
        if (req != null) req.setAttribute(CACHE_KEY, tc);
        return tc;
    }

    public List<String> assignedDistricts() {
        Telecaller tc = currentTelecaller();
        if (tc.getDistricts() == null) return List.of();
        return tc.getDistricts().stream()
                .filter(d -> d != null && !d.trim().isEmpty())
                .map(String::trim).distinct().toList();
    }

    public boolean hasDistrictAccess(String district) {
        if (district == null || district.trim().isEmpty()) return false;
        return assignedDistricts().stream().anyMatch(d -> d.equalsIgnoreCase(district.trim()));
    }

    public Lead requireLeadAccess(String leadId) {
        Lead lead = leadRepository.findByLeadId(leadId)
                .orElseThrow(() -> new AccessDeniedException("Lead not found or access denied"));
        if (!hasDistrictAccess(lead.getDistrict()))
            throw new AccessDeniedException("You are not assigned to the district of this lead");
        return lead;
    }

    public void requireDistrictAccess(String district) {
        if (!hasDistrictAccess(district))
            throw new AccessDeniedException("You are not assigned to district: " + district);
    }

    public Telecaller currentTelecallerByUsername(String username) {
        return telecallerRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Telecaller account not found"));
    }

    public void requireLeadAccessForUsername(String username, String leadId) {
        Telecaller tc = currentTelecallerByUsername(username);
        Lead lead = leadRepository.findByLeadId(leadId)
                .orElseThrow(() -> new AccessDeniedException("Lead not found"));
        boolean allowed = tc.getDistricts() != null && lead.getDistrict() != null &&
                tc.getDistricts().stream().anyMatch(d -> d != null && d.equalsIgnoreCase(lead.getDistrict()));
        if (!allowed) throw new AccessDeniedException("Telecaller is not assigned to this lead district");
    }

    public boolean isTelecaller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TELECALLER"));
    }
}