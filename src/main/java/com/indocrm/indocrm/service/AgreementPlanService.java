// FILE: src/main/java/com/indocrm/indocrm/service/AgreementPlanService.java
package com.indocrm.indocrm.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.dto.AgreementPlanRequest;
import com.indocrm.indocrm.model.AgreementPlan;
import com.indocrm.indocrm.repository.AgreementPlanRepository;

@Service
public class AgreementPlanService {

    private static final Logger log =
            LoggerFactory.getLogger(AgreementPlanService.class);

    public static final String TYPE_ERP = "ERP";
    public static final String TYPE_DIGITAL_MARKETING = "DIGITAL_MARKETING";

    private final AgreementPlanRepository repo;

    public AgreementPlanService(AgreementPlanRepository repo) {
        this.repo = repo;
        // NOTE: Do NOT seed in the constructor. If Mongo is not yet
        // reachable at context-refresh time, a seeding query will
        // abort startup with the exact "Cannot resolve reference to
        // bean 'mongoTemplate'" error we're fixing. Seeding happens
        // in onApplicationReady() below.
    }

    /**
     * Runs once the Spring context is fully up and the Mongo
     * connection has been validated. Any failure here is logged
     * but does NOT stop the application — the app can still serve
     * requests, and seeding will be retried on the next restart.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            seedDefaultsIfEmpty();
        } catch (Exception e) {
            log.warn("AgreementPlan seeding skipped (Mongo not ready yet): {}",
                    e.getMessage());
        }
    }

    private boolean admin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void requireAdmin() {
        if (!admin()) {
            throw new AccessDeniedException(
                    "Only Super Admin can manage agreement plans");
        }
    }

    public List<AgreementPlan> listByType(String agreementType) {
        if (agreementType == null || agreementType.isBlank()) {
            return repo.findAllByOrderByAgreementTypeAscDisplayOrderAsc();
        }
        return repo.findByAgreementTypeAndActiveTrueOrderByDisplayOrderAsc(
                agreementType.trim().toUpperCase());
    }

    public AgreementPlan get(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plan not found"));
    }

    public AgreementPlan create(AgreementPlanRequest r) {
        requireAdmin();
        validate(r);
        AgreementPlan p = new AgreementPlan();
        apply(p, r);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    public AgreementPlan update(String id, AgreementPlanRequest r) {
        requireAdmin();
        validate(r);
        AgreementPlan p = get(id);
        apply(p, r);
        p.setUpdatedAt(Instant.now());
        return repo.save(p);
    }

    public void archive(String id) {
        requireAdmin();
        AgreementPlan p = get(id);
        p.setActive(false);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
    }

    private void validate(AgreementPlanRequest r) {
        if (r.getAgreementType() == null
                || !(TYPE_ERP.equalsIgnoreCase(r.getAgreementType())
                || TYPE_DIGITAL_MARKETING.equalsIgnoreCase(
                        r.getAgreementType()))) {
            throw new IllegalArgumentException(
                    "agreementType must be ERP or DIGITAL_MARKETING");
        }
        if (r.getPlanName() == null || r.getPlanName().isBlank()) {
            throw new IllegalArgumentException("Plan name is required");
        }
        if (r.getFeeAmount() < 0) {
            throw new IllegalArgumentException(
                    "Fee amount cannot be negative");
        }
    }

    private void apply(AgreementPlan p, AgreementPlanRequest r) {
        p.setAgreementType(r.getAgreementType().trim().toUpperCase());
        p.setPlanName(r.getPlanName().trim());
        p.setDescription(r.getDescription());
        p.setFeeAmount(r.getFeeAmount());
        p.setDiscountAmount(Math.max(0, r.getDiscountAmount()));
        p.setGstPercent(r.getGstPercent() <= 0 ? 18.0 : r.getGstPercent());
        p.setMinStudentStrength(r.getMinStudentStrength());
        p.setMaxStudentStrength(r.getMaxStudentStrength());
        p.setDisplayOrder(r.getDisplayOrder());
    }

    /**
     * Seeds the default DM plan and the ERP tiers.
     * Called from onApplicationReady() — never from the constructor.
     */
    private void seedDefaultsIfEmpty() {
        if (repo.countByAgreementType(TYPE_DIGITAL_MARKETING) == 0) {
            AgreementPlan dm = new AgreementPlan();
            dm.setAgreementType(TYPE_DIGITAL_MARKETING);
            dm.setPlanName("Standard Digital Marketing Plan");
            dm.setDescription(
                    "Social media management, Meta Ads, admission "
                            + "campaigns, creatives, admission dashboard (no cost).");
            dm.setFeeAmount(23000);
            dm.setDiscountAmount(7000);
            dm.setGstPercent(18);
            dm.setDisplayOrder(1);
            dm.setSystemPlan(true);
            dm.setCreatedAt(Instant.now());
            dm.setUpdatedAt(Instant.now());
            repo.save(dm);
        }

        if (repo.countByAgreementType(TYPE_ERP) == 0) {
            int[][] tiers = {
                    { 1, 150, 10000 },
                    { 151, 250, 12000 },
                    { 251, 400, 16000 },
                    { 401, 600, 22000 },
                    { 601, -1, 30000 },
            };
            int order = 1;
            for (int[] tier : tiers) {
                AgreementPlan erp = new AgreementPlan();
                erp.setAgreementType(TYPE_ERP);
                String label = tier[1] == -1
                        ? (tier[0] + "+ Students")
                        : (tier[0] + "-" + tier[1] + " Students");
                erp.setPlanName("ERP Plan – " + label);
                erp.setDescription("School ERP / web dashboard subscription for "
                        + label.toLowerCase() + ".");
                erp.setFeeAmount(tier[2]);
                erp.setGstPercent(18);
                erp.setMinStudentStrength(tier[0]);
                erp.setMaxStudentStrength(tier[1] == -1 ? null : tier[1]);
                erp.setDisplayOrder(order++);
                erp.setSystemPlan(true);
                erp.setCreatedAt(Instant.now());
                erp.setUpdatedAt(Instant.now());
                repo.save(erp);
            }
        }
    }
}