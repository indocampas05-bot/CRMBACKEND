// FILE: src/main/java/com/indocrm/indocrm/service/AgreementService.java
// REPLACES existing file.
//
// Changes from the original:
//  - apply() now requires a planId, resolves the AgreementPlan, validates it
//    against the template's agreementType, computes pricing (never
//    hard-coded), and builds the full merge-field map the two templates in
//    AgreementTemplateBootstrapService need (agreement date, client name/
//    address/pincode, plan name, fee/discount/net/GST/total, student
//    strength, authorized signatory, signature date).
//  - new preview(AgreementRequest) — same merge logic, nothing persisted,
//    used by the "Preview Agreement" step before Create Agreement.
//  - create()/update() now also render a PDF via AgreementPdfService and
//    store its path, so /api/agreements/{id}/pdf has something to serve.
package com.indocrm.indocrm.service;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.dto.AgreementRequest;
import com.indocrm.indocrm.model.Agreement;
import com.indocrm.indocrm.model.AgreementHistory;
import com.indocrm.indocrm.model.AgreementPlan;
import com.indocrm.indocrm.model.AgreementStatus;
import com.indocrm.indocrm.model.AgreementTemplate;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.repository.AgreementPlanRepository;
import com.indocrm.indocrm.repository.AgreementRepository;
import com.indocrm.indocrm.repository.AgreementTemplateRepository;
import com.indocrm.indocrm.repository.LeadRepository;

@Service
public class AgreementService {

    private final AgreementRepository repo;
    private final LeadRepository leads;
    private final AgreementTemplateRepository templates;
    private final AgreementPlanRepository plans;
    private final AgreementPdfService pdfService;
    private final TelecallerAccessService access;
    private final SimpMessagingTemplate ws;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public AgreementService(
            AgreementRepository r,
            LeadRepository l,
            AgreementTemplateRepository t,
            AgreementPlanRepository p,
            AgreementPdfService pdfService,
            TelecallerAccessService a,
            SimpMessagingTemplate w
    ) {
        repo = r;
        leads = l;
        templates = t;
        plans = p;
        this.pdfService = pdfService;
        access = a;
        ws = w;
    }

    private String user() {
        return access.currentUsername();
    }

    private boolean admin() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_SUPER_ADMIN")
                                || a.getAuthority().equals("ROLE_ADMIN")
                );
    }

    /** Lists agreements. */
    public List<Agreement> list(String status, String telecaller, String leadId) {
        List<Agreement> agreements = admin()
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByCreatedByOrderByCreatedAtDesc(user());

        return agreements.stream()
                .filter(a -> status == null || status.isBlank()
                        || a.getStatus().name().equalsIgnoreCase(status)
                        || a.getStatus().name().replace('_', ' ').equalsIgnoreCase(status))
                .filter(a -> telecaller == null || telecaller.isBlank() || telecaller.equals(a.getCreatedBy()))
                .filter(a -> leadId == null || leadId.isBlank() || leadId.equals(a.getLeadId()))
                .toList();
    }

    /** Gets one agreement with access control. */
    public Agreement get(String id) {
        Agreement a = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found"));
        if (!admin() && !user().equals(a.getCreatedBy())) {
            throw new AccessDeniedException("Agreement access denied");
        }
        return a;
    }

    /** Creates agreement. */
    public Agreement create(AgreementRequest r) {
        Lead l = admin() ? lead(r.getLeadId()) : access.requireLeadAccess(r.getLeadId());

        Agreement a = new Agreement();
        a.setCreatedBy(user());
        a.setCreatedByName(user());
        a.setCreatedAt(Instant.now());

        apply(a, l, r);
        history(a, "Created agreement", null);
        Agreement saved = save(a);
        regeneratePdfBestEffort(saved);
        return saved;
    }

    /** Updates agreement. */
    public Agreement update(String id, AgreementRequest r) {
        Agreement a = get(id);

        if (!admin() && !Set.of(AgreementStatus.DRAFT, AgreementStatus.REJECTED).contains(a.getStatus())) {
            throw new IllegalStateException("Only Draft or Rejected agreements can be edited");
        }

        Lead l = admin() ? lead(r.getLeadId()) : access.requireLeadAccess(r.getLeadId());

        apply(a, l, r);

        if (a.getStatus() == AgreementStatus.REJECTED) {
            a.setStatus(AgreementStatus.DRAFT);
        }

        a.setVersion(a.getVersion() + 1);
        history(a, "Updated agreement", null);
        Agreement saved = save(a);
        regeneratePdfBestEffort(saved);
        return saved;
    }

    /**
     * Preview — runs the exact same merge/pricing logic as create(), but
     * persists nothing. Used by the "Preview Agreement" step so the
     * telecaller can review the filled-in agreement before committing it.
     */
    public Map<String, Object> preview(AgreementRequest r) {
        Lead l = admin() ? lead(r.getLeadId()) : access.requireLeadAccess(r.getLeadId());
        Agreement scratch = new Agreement();
        apply(scratch, l, r);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contentHtml", scratch.getContentHtml());
        result.put("agreementType", scratch.getAgreementType());
        result.put("planName", scratch.getPlanName());
        result.put("feeAmount", scratch.getFeeAmount());
        result.put("discountAmount", scratch.getDiscountAmount());
        result.put("gstPercent", scratch.getGstPercent());
        result.put("gstAmount", scratch.getGstAmount());
        result.put("netPayable", scratch.getNetPayable());
        result.put("totalPayable", scratch.getTotalPayable());
        result.put("studentStrength", scratch.getStudentStrength());
        return result;
    }

    /** Deletes agreement. Super Admin can delete any; Telecaller only own Draft/Rejected. */
    public void delete(String id) {
        Agreement a = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found"));

        if (!admin()) {
            if (!user().equals(a.getCreatedBy())) {
                throw new AccessDeniedException("Agreement access denied");
            }
            if (!Set.of(AgreementStatus.DRAFT, AgreementStatus.REJECTED).contains(a.getStatus())) {
                throw new IllegalStateException("Only Draft or Rejected agreements can be deleted");
            }
        }

        repo.delete(a);
        pdfService.deleteQuietly(a.getGeneratedPdfPath());

        try {
            ws.convertAndSend("/topic/agreements", Map.of("event", "DELETED", "id", id));
        } catch (Exception ignored) {
        }
    }

    /** Submits agreement for Super Admin approval. */
    public Agreement submit(String id) {
        Agreement a = get(id);

        if (admin() || !user().equals(a.getCreatedBy())) {
            throw new AccessDeniedException("Only the Telecaller who created the agreement can submit it");
        }
        if (!Set.of(AgreementStatus.DRAFT, AgreementStatus.REJECTED).contains(a.getStatus())) {
            throw new IllegalStateException("Only Draft or Rejected agreements can be submitted");
        }

        a.setStatus(a.getSubmittedAt() == null ? AgreementStatus.PENDING_APPROVAL : AgreementStatus.RESUBMITTED);
        a.setSubmittedAt(Instant.now());
        a.setRejectionReason(null);

        history(a, a.getStatus() == AgreementStatus.RESUBMITTED ? "Resubmitted for approval" : "Submitted for Super Admin approval", null);
        return save(a);
    }

    /** Approves agreement. */
    public Agreement approve(String id, String remark) {
        requireAdmin();
        Agreement a = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Agreement not found"));

        if (!Set.of(AgreementStatus.PENDING_APPROVAL, AgreementStatus.RESUBMITTED).contains(a.getStatus())) {
            throw new IllegalStateException("Agreement is not waiting for approval");
        }

        a.setStatus(AgreementStatus.APPROVED);
        a.setApprovedBy(user());
        a.setApprovedAt(Instant.now());
        a.setApprovalRemark(remark);

        history(a, "Approved", remark);
        return save(a);
    }

    /** Rejects agreement. */
    public Agreement reject(String id, String reason) {
        requireAdmin();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        Agreement a = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Agreement not found"));

        if (!Set.of(AgreementStatus.PENDING_APPROVAL, AgreementStatus.RESUBMITTED).contains(a.getStatus())) {
            throw new IllegalStateException("Agreement is not waiting for approval");
        }

        a.setStatus(AgreementStatus.REJECTED);
        a.setRejectedBy(user());
        a.setRejectedAt(Instant.now());
        a.setRejectionReason(reason);

        history(a, "Rejected", reason);
        return save(a);
    }

    /** Sends approved agreement to lead. */
    public Agreement send(String id, String channel) {
        Agreement a = get(id);

        if (admin() || !user().equals(a.getCreatedBy())) {
            throw new AccessDeniedException("Only the creating Telecaller can send this agreement");
        }
        if (a.getStatus() != AgreementStatus.APPROVED) {
            throw new IllegalStateException("Super Admin approval is mandatory before sending");
        }

        a.setStatus(AgreementStatus.SENT);
        a.setSentBy(user());
        a.setSentAt(Instant.now());
        a.setSentChannel(channel == null ? "WhatsApp" : channel);
        a.setSentTo(a.getPhone());

        history(a, "Sent to lead via " + a.getSentChannel(), null);
        return save(a);
    }

   
    public java.io.File pdfFile(String id) {
        Agreement a = get(id);
        regeneratePdfBestEffort(a);
        a = repo.save(a);
        if (a.getGeneratedPdfPath() == null || !new java.io.File(a.getGeneratedPdfPath()).exists()) {
            throw new IllegalStateException("Unable to generate PDF for this agreement");
        }
        return new java.io.File(a.getGeneratedPdfPath());
    }
    private void regeneratePdfBestEffort(Agreement a) {
        try {
            String path = pdfService.generate(a);
            a.setGeneratedPdfPath(path);
            a.setGeneratedPdfFileName("Agreement_" + a.getId() + ".pdf");
        } catch (Exception e) {
            // Never block create/update on PDF rendering failure — the HTML
            // content is already saved and previewable; the PDF can be
            // regenerated on next GET /{id}/pdf.
        }
    }

    /**
     * Applies request data + selected plan pricing to the agreement, then
     * merges everything into the template's contentHtml.
     */
    private void apply(Agreement a, Lead l, AgreementRequest r) {

        // Built-in category agreements (Digital Marketing / Admin Dashboard / Pro / Pro Plus / Pro Max):
        // the editable document is built in the browser, so no DB template / plan is needed.
        if (r.getTemplateId() != null && r.getTemplateId().startsWith("builtin-")) {
            applyBuiltin(a, l, r);
            return;
        }

        if (r.getTemplateId() == null || r.getTemplateId().isBlank()) {
            throw new IllegalArgumentException("Please select an agreement template");
        }
        AgreementTemplate t = templates.findById(r.getTemplateId())
                .filter(AgreementTemplate::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Selected template not found"));

        if (r.getPlanId() == null || r.getPlanId().isBlank()) {
            throw new IllegalArgumentException("Please select a plan");
        }
        AgreementPlan plan = plans.findById(r.getPlanId())
                .filter(AgreementPlan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Selected plan not found"));

        String agreementType = t.getAgreementType() != null ? t.getAgreementType() : plan.getAgreementType();
        if (agreementType != null && plan.getAgreementType() != null
                && !agreementType.equalsIgnoreCase(plan.getAgreementType())) {
            throw new IllegalArgumentException(
                    "The selected plan (" + plan.getAgreementType() + ") does not match the selected template ("
                            + agreementType + ")");
        }

        boolean isErp = "ERP".equalsIgnoreCase(agreementType);

        // ---- client details (lead-derived, overridable by the form) ----
        String formClientName = r.getMergeValues() == null ? null : r.getMergeValues().get("client_name");
        String formClientAddress = r.getMergeValues() == null ? null : r.getMergeValues().get("client_address");

        a.setLeadId(l.getLeadId());
        a.setClientName(nonBlank(formClientName, l.getSchool(), l.getName()));
        a.setOrganization(nonBlank(l.getSchool(), l.getName()));
        a.setEmail(l.getEmail());
        a.setPhone(nonBlank(l.getMobile(), l.getPhoneNumber()));
        a.setAddress(nonBlank(formClientAddress, l.getSchoolAddress(), l.getArea()));
        a.setPincode(n(r.getPincode()));

        a.setTemplateId(t.getId());
        a.setAgreementType(agreementType);
        a.setTitle(blank(r.getTitle()) ? t.getName() : r.getTitle());

        String agreementDate = blank(r.getAgreementDate()) ? LocalDate.now().format(ISO_DATE) : r.getAgreementDate();
        a.setAgreementDate(agreementDate);
        a.setAuthorizedPersonName(n(r.getAuthorizedPersonName()));
        a.setAuthorizedPersonDesignation(n(r.getAuthorizedPersonDesignation()));

        // ---- pricing, resolved from the plan, never hard-coded ----
        a.setPlanId(plan.getId());
        a.setPlanName(plan.getPlanName());
        a.setGstPercent(plan.getGstPercent());

        Map<String, String> m = new LinkedHashMap<>();
        if (r.getMergeValues() != null) m.putAll(r.getMergeValues());

        m.put("agreement_date", displayDate(agreementDate));
        m.put("signature_date", displayDate(agreementDate));
        m.put("client_name", n(a.getClientName()));
        m.put("client_address", n(a.getAddress()));
        m.put("pincode", n(a.getPincode()));
        m.put("authorized_person_name", n(a.getAuthorizedPersonName()));
        m.put("authorized_person_designation", n(a.getAuthorizedPersonDesignation()));
        m.put("plan_name", n(plan.getPlanName()));
        m.put("gst_percent", trimNum(plan.getGstPercent()));

        if (isErp) {
            Integer strength = r.getStudentStrength();
            if (strength == null || strength <= 0) {
                throw new IllegalArgumentException("Please enter the student strength");
            }
            if (plan.getMinStudentStrength() != null && strength < plan.getMinStudentStrength()
                    || plan.getMaxStudentStrength() != null && strength > plan.getMaxStudentStrength()) {
                throw new IllegalArgumentException(
                        "Student strength (" + strength + ") is outside the range for plan \"" + plan.getPlanName() + "\"");
            }
            a.setStudentStrength(strength);

            double fee = plan.getFeeAmount();
            double gst = round2(fee * plan.getGstPercent() / 100.0);
            double total = round2(fee + gst);

            a.setFeeAmount(fee);
            a.setGstAmount(gst);
            a.setTotalPayable(total);

            m.put("student_strength", String.valueOf(strength));
            m.put("subscription_fee", money(fee));
            m.put("gst_amount", money(gst));
            m.put("total_payable", money(total));
        } else {
            double fee = plan.getFeeAmount();
            double discount = r.getDiscountOverride() != null ? Math.max(0, r.getDiscountOverride()) : plan.getDiscountAmount();
            double net = round2(Math.max(0, fee - discount));

            a.setFeeAmount(fee);
            a.setDiscountAmount(discount);
            a.setNetPayable(net);

            m.put("yearly_fee", money(fee));
            m.put("discount_amount", money(discount));
            m.put("net_payable", money(net));
        }

        String source = blank(r.getContentHtml()) ? t.getContentHtml() : r.getContentHtml();
        a.setContentHtml(merge(source, m));

        a.setHeaderHtml(r.getHeaderHtml());
        a.setFooterHtml(r.getFooterHtml());
        a.setLogoDataUrl(r.getLogoDataUrl());
        a.setLetterPadDataUrl(r.getLetterPadDataUrl());
        a.setMergeValues(m);
        a.setUpdatedAt(Instant.now());
    }


    /** Built-in (WYSIWYG-edited) agreement: stores the edited document exactly as sent. */
    private void applyBuiltin(Agreement a, Lead l, AgreementRequest r) {
        if (blank(r.getContentHtml())) {
            throw new IllegalArgumentException("Agreement content is empty");
        }
        Map<String, String> m = new LinkedHashMap<>();
        if (r.getMergeValues() != null) m.putAll(r.getMergeValues());

        a.setLeadId(l.getLeadId());
        a.setClientName(nonBlank(m.get("client_name"), l.getSchool(), l.getName()));
        a.setOrganization(nonBlank(l.getSchool(), l.getName()));
        a.setEmail(l.getEmail());
        a.setPhone(nonBlank(l.getMobile(), l.getPhoneNumber()));
        a.setAddress(nonBlank(m.get("client_address"), l.getSchoolAddress(), l.getArea()));
        a.setPincode(n(r.getPincode()));

        a.setTemplateId(r.getTemplateId());
        a.setAgreementType(nonBlank(r.getAgreementType(), "ERP"));
        a.setServiceType(n(r.getServiceType()));
        a.setPlanId(null);
        a.setPlanName(n(r.getServiceType()));
        a.setTitle(blank(r.getTitle()) ? n(r.getServiceType()) + " Agreement" : r.getTitle());

        String agreementDate = blank(r.getAgreementDate()) ? LocalDate.now().format(ISO_DATE) : r.getAgreementDate();
        a.setAgreementDate(agreementDate);
        a.setAuthorizedPersonName(n(r.getAuthorizedPersonName()));
        a.setAuthorizedPersonDesignation(n(r.getAuthorizedPersonDesignation()));

        m.put("agreement_date", displayDate(agreementDate));
        a.setContentHtml(r.getContentHtml());      // no {{merge}} - content is final
        a.setHeaderHtml(null);
        a.setFooterHtml(null);
        a.setMergeValues(m);
        a.setUpdatedAt(Instant.now());
    }

    private String displayDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate, ISO_DATE).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            return isoDate;
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String money(double v) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("en", "IN"));
        return nf.format(Math.round(v));
    }

    private String trimNum(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Replaces {{variable}} with merge values. Values are HTML/XML-escaped
     * on the way in — they're plain data (client names, addresses, etc.),
     * never markup, and openhtmltopdf parses the final document as strict
     * XML, so an unescaped "&" or "<" in e.g. a school name like "R&D Public
     * School" would break PDF generation the same way the named entities in
     * the static template text did (see AgreementPdfService.normalizeEntities). */
    private String merge(String html, Map<String, String> values) {
        if (html == null) return "";
        Matcher m = Pattern.compile("\\{\\{\\s*([^{}\\s]+)\\s*\\}\\}").matcher(html);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String value = escapeXml(values.getOrDefault(m.group(1), ""));
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }

    private String escapeXml(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String n(String value) {
        return value == null ? "" : value;
    }

    private Lead lead(String id) {
        return leads.findByLeadId(id).orElseThrow(() -> new IllegalArgumentException("Lead not found"));
    }

    private void requireAdmin() {
        if (!admin()) {
            throw new AccessDeniedException("Only Super Admin can approve or reject agreements");
        }
    }

    /** Adds agreement history. */
    private void history(Agreement a, String action, String remarks) {
        AgreementHistory h = new AgreementHistory();
        h.setAction(action);
        h.setActor(user());
        h.setActorRole(admin() ? "SUPER_ADMIN" : "TELECALLER");
        h.setRemarks(remarks);
        h.setStatus(a.getStatus().name());
        h.setAt(Instant.now());
        h.setVersion(a.getVersion());
        a.getHistory().add(h);
    }

    /** Saves agreement and broadcasts WebSocket update. */
    private Agreement save(Agreement a) {
        Agreement saved = repo.save(a);
        try {
            ws.convertAndSend("/topic/agreements", saved);
        } catch (Exception ignored) {
        }
        return saved;
    }
}

