package com.indocrm.indocrm.service;

import java.time.Instant;
import java.time.LocalDate;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.indocrm.indocrm.dto.QuotationRequest;
import com.indocrm.indocrm.model.Lead;
import com.indocrm.indocrm.model.Quotation;
import com.indocrm.indocrm.model.QuotationTemplate;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateField;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.repository.QuotationRepository;

@Service
public class QuotationService {

    // Canonical approval-workflow status values.
    public static final String STATUS_DRAFT = "Draft";
    public static final String STATUS_PENDING_APPROVAL = "Pending Approval";
    public static final String STATUS_APPROVED = "Approved";
    public static final String STATUS_REJECTED = "Rejected";
    public static final String STATUS_REVISION_REQUIRED = "Revision Required";
    public static final String STATUS_SENT = "Sent";

    private final QuotationRepository repo;
    private final QuotationTemplateService templateService;
    private final QuotationPdfService pdfService;
    private final TelecallerAccessService access;
    private final MongoTemplate mongo;

    // First number handed out in a new year. The latest hand-made PDF was ...072, so continue from 073.
    @Value("${quotation.number.start:73}")
    private int numberStart;

    public QuotationService(QuotationRepository repo, QuotationTemplateService templateService,
                             QuotationPdfService pdfService, TelecallerAccessService access,
                             MongoTemplate mongo) {
        this.mongo = mongo;
        this.repo = repo;
        this.templateService = templateService;
        this.pdfService = pdfService;
        this.access = access;
    }

    /*
     * ============================================================
     * TELECALLER — own quotations only
     * ============================================================
     */

    public List<Quotation> list() {
        return repo.findByCreatedByOrderByCreatedAtDesc(access.currentUsername());
    }

    public Quotation get(String id) {
        Quotation q = repo.findById(id).orElseThrow(() -> new RuntimeException("Quotation not found: " + id));
        checkOwner(q);
        return q;
    }

    public Quotation create(QuotationRequest r) throws Exception {
        String username = access.currentUsername();
        Lead lead = access.requireLeadAccess(r.getLeadId());
        QuotationTemplate template = templateService.resolveForQuotation(r.getTemplateId(), r.getServiceType());

        Quotation q = new Quotation();
        q.setQuotationNo(nextQuotationNo());
        q.setCreatedBy(username);
        q.setTelecallerName(currentTelecallerDisplayName(username));
        q.setStatus(STATUS_DRAFT);
        applyFromLeadAndRequest(q, lead, template, r);
        // A telecaller cannot skip the approval workflow by posting a status directly.
        q.setStatus(STATUS_DRAFT);
        q = repo.save(q);

        String pdfPath;
        try {
            pdfPath = pdfService.generate(q, template);
        } catch (Exception e) {
            // Do not leave a half-created quotation (no PDF) behind when generation fails.
            repo.deleteById(q.getId());
            throw e;
        }
        q.setGeneratedPdfPath(pdfPath);
        q.setGeneratedPdfFileName("Quotation_" + (q.getQuotationNo() != null
                ? q.getQuotationNo().replace('/', '-') : q.getId()) + ".pdf");
        addTimeline(q, "Quotation created", "document", "#dbeafe", "#1d4ed8");
        return repo.save(q);
    }

    public Quotation update(String id, QuotationRequest r) throws Exception {
        Quotation q = get(id);
        requireEditable(q);
        Lead lead = access.requireLeadAccess(r.getLeadId());
        QuotationTemplate template = templateService.resolveForQuotation(r.getTemplateId(), r.getServiceType());
        String statusBefore = q.getStatus();
        applyFromLeadAndRequest(q, lead, template, r);
        // Editing never jumps the workflow forward — only the submit/review/send
        // actions change status. A Rejected or Revision Required quotation drops
        // back to Draft so the telecaller can resubmit it deliberately, and every
        // resubmission goes through the Super Admin confirmation process again.
        boolean wasRejectedOrRevision = STATUS_REJECTED.equalsIgnoreCase(statusBefore)
                || STATUS_REVISION_REQUIRED.equalsIgnoreCase(statusBefore);
        q.setStatus(wasRejectedOrRevision ? STATUS_DRAFT : statusBefore);
        if (wasRejectedOrRevision) {
            // Clear the previous remarks now that the telecaller has actioned them;
            // they remain visible in the timeline history below.
            q.setRejectionReason(null);
            q.setRevisionNotes(null);
        }

        String pdfPath = pdfService.generate(q, template);
        q.setGeneratedPdfPath(pdfPath);
        addTimeline(q, "Quotation updated & PDF regenerated", "refresh", "#fef3c7", "#b45309");
        return repo.save(q);
    }

    /** Generic status patch — kept for lightweight, non-workflow states (e.g. "Viewed").
     *  The approval-pipeline transitions (Pending Approval / Approved / Rejected / Sent)
     *  must go through submitForApproval() / review() / markSent() so the audit fields
     *  (submittedAt, reviewedBy, sentAt, ...) stay correct. */
    public Quotation updateStatus(String id, String status) {
        if (STATUS_PENDING_APPROVAL.equalsIgnoreCase(status) || STATUS_APPROVED.equalsIgnoreCase(status)
                || STATUS_REJECTED.equalsIgnoreCase(status) || STATUS_REVISION_REQUIRED.equalsIgnoreCase(status)
                || STATUS_SENT.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Use the submit/review/send actions to change to " + status + ".");
        }
        Quotation q = get(id);
        q.setStatus(status);
        addTimeline(q, "Status changed to " + status, statusIcon(status), "#e0e7ff", "#4338ca");
        return repo.save(q);
    }

    /*
     * ============================================================
     * TELECALLER — submit for approval
     * ============================================================
     */
    public Quotation submitForApproval(String id) {
        Quotation q = get(id);
        if (!STATUS_DRAFT.equalsIgnoreCase(q.getStatus())) {
            throw new IllegalStateException("Only a Draft quotation can be submitted for approval.");
        }
        q.setStatus(STATUS_PENDING_APPROVAL);
        q.setSubmittedAt(Instant.now());
        q.setTelecallerName(currentTelecallerDisplayName(access.currentUsername()));
        q.setReviewedAt(null);
        q.setReviewedBy(null);
        q.setRejectionReason(null);
        q.setRevisionNotes(null);
        addTimeline(q, "Submitted for Super Admin approval", "send", "#e0e7ff", "#4338ca");
        return repo.save(q);
    }

    /*
     * ============================================================
     * TELECALLER — send the approved quotation to the lead
     * ============================================================
     */
    public Quotation markSent(String id, String channel) {
        Quotation q = get(id);

        // A quotation remains eligible for future delivery after its first send.
        // "Sent" is a delivery state, not a terminal state, so Approved and Sent
        // quotations can both be sent/resend through WhatsApp or Email.
        boolean approved = STATUS_APPROVED.equalsIgnoreCase(q.getStatus());
        boolean alreadySent = STATUS_SENT.equalsIgnoreCase(q.getStatus());
        if (!approved && !alreadySent) {
            throw new IllegalStateException(
                    "Only an Approved or previously Sent quotation can be delivered to the lead.");
        }

        q.setStatus(STATUS_SENT);
        q.setSentAt(Instant.now());
        q.setSentChannel(channel);

        String action = alreadySent ? "Resent via " + channel : "Sent via " + channel;
        addTimeline(q, action,
                "WhatsApp".equalsIgnoreCase(channel) ? "message" : "mail",
                "#dcfce7", "#15803d");
        return repo.save(q);
    }

    public void delete(String id) {
        Quotation q = get(id);
        try {
            if (q.getGeneratedPdfPath() != null) java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(q.getGeneratedPdfPath()));
        } catch (Exception ignored) {}
        repo.deleteById(id);
    }

    public java.io.File pdfFile(String id) {
        return freshPdf(get(id));
    }

    /** Always draws the PDF again from the saved quotation data, so the current letterpad / logo / footer
     *  and layout are used - old PDFs created before a letterhead change never show up stale.
     *  Falls back to the stored file only if generating fails. */
    private java.io.File freshPdf(Quotation q) {
        try {
            QuotationTemplate t = templateService.resolveForQuotation(q.getTemplateId(), q.getServiceType());
            String path = pdfService.generate(q, t);
            if (!path.equals(q.getGeneratedPdfPath())) {
                q.setGeneratedPdfPath(path);
                repo.save(q);
            }
            return new java.io.File(path);
        } catch (Exception e) {
            if (q.getGeneratedPdfPath() != null && new java.io.File(q.getGeneratedPdfPath()).isFile()) {
                return new java.io.File(q.getGeneratedPdfPath());
            }
            throw new RuntimeException("Unable to generate the quotation PDF: " + e.getMessage());
        }
    }

    /*
     * ============================================================
     * SUPER ADMIN — visibility across all telecallers
     * ============================================================
     */

    public List<Quotation> adminList(String status) {
        if (status == null || status.isBlank() || "All".equalsIgnoreCase(status)) {
            return repo.findAllByOrderByCreatedAtDesc();
        }
        return repo.findByStatusOrderByCreatedAtDesc(status);
    }

    public Map<String, Object> adminSummary() {
        List<Quotation> all = repo.findAllByOrderByCreatedAtDesc();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", all.size());
        summary.put("draft", all.stream().filter(q -> STATUS_DRAFT.equalsIgnoreCase(q.getStatus())).count());
        summary.put("pendingApproval", all.stream().filter(q -> STATUS_PENDING_APPROVAL.equalsIgnoreCase(q.getStatus())).count());
        summary.put("approved", all.stream().filter(q -> STATUS_APPROVED.equalsIgnoreCase(q.getStatus())).count());
        summary.put("rejected", all.stream().filter(q -> STATUS_REJECTED.equalsIgnoreCase(q.getStatus())).count());
        summary.put("revisionRequired", all.stream().filter(q -> STATUS_REVISION_REQUIRED.equalsIgnoreCase(q.getStatus())).count());
        summary.put("sent", all.stream().filter(q -> STATUS_SENT.equalsIgnoreCase(q.getStatus())).count());
        return summary;
    }

    /** Fetch without ownership restriction — for Super Admin review/PDF access only. */
    public Quotation getForAdmin(String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Quotation not found: " + id));
    }

    public java.io.File pdfFileForAdmin(String id) {
        return freshPdf(getForAdmin(id));
    }

    /*
     * ============================================================
     * SUPER ADMIN — approve / reject / request changes
     * ============================================================
     */
    /**
     * @param status  "Approved" | "Rejected" | "Revision Required"
     * @param remarks required for Rejected (rejection reason) and for
     *                Revision Required (what needs to change); ignored for Approved.
     */
    public Quotation review(String id, String status, String remarks) {
        boolean isApprove = STATUS_APPROVED.equalsIgnoreCase(status);
        boolean isReject = STATUS_REJECTED.equalsIgnoreCase(status);
        boolean isRevision = STATUS_REVISION_REQUIRED.equalsIgnoreCase(status);
        if (!isApprove && !isReject && !isRevision) {
            throw new IllegalArgumentException("Status must be Approved, Rejected or Revision Required");
        }
        if ((isReject || isRevision) && (remarks == null || remarks.isBlank())) {
            throw new IllegalArgumentException(isReject
                    ? "A rejection reason is required."
                    : "Please describe the changes you'd like the telecaller to make.");
        }
        Quotation q = getForAdmin(id);
        if (!STATUS_PENDING_APPROVAL.equalsIgnoreCase(q.getStatus())) {
            throw new IllegalStateException("Only quotations Pending Confirmation can be reviewed.");
        }

        String finalStatus = isApprove ? STATUS_APPROVED : isReject ? STATUS_REJECTED : STATUS_REVISION_REQUIRED;
        q.setStatus(finalStatus);
        q.setReviewedAt(Instant.now());
        q.setReviewedBy(currentUsernameOrNull());
        q.setRejectionReason(isReject ? remarks : null);
        q.setRevisionNotes(isRevision ? remarks : null);

        String action = isApprove ? "Approved by Super Admin"
                : isReject ? ("Rejected by Super Admin" + (remarks != null && !remarks.isBlank() ? (": " + remarks) : ""))
                : ("Revision requested by Super Admin" + (remarks != null && !remarks.isBlank() ? (": " + remarks) : ""));
        String bg = isApprove ? "#dcfce7" : isReject ? "#fee2e2" : "#fef9c3";
        String color = isApprove ? "#15803d" : isReject ? "#dc2626" : "#92400e";
        addTimeline(q, action, statusIcon(finalStatus), bg, color);
        return repo.save(q);
    }

    private void applyFromLeadAndRequest(Quotation q, Lead lead, QuotationTemplate template, QuotationRequest r) {
        q.setLeadId(lead.getLeadId());
        q.setTemplateId(template.getId());
        q.setClientName(firstNonBlank(r.getClientName(), lead.getName()));
        q.setOrganization(firstNonBlank(r.getOrganization(), lead.getSchool(), lead.getName()));
        q.setEmail(firstNonBlank(r.getEmail(), lead.getEmail()));
        q.setPhone(firstNonBlank(r.getPhone(), lead.getMobile(), lead.getPhoneNumber()));
        q.setAddress(firstNonBlank(r.getAddress(), lead.getSchoolAddress(), lead.getArea()));
        q.setServiceType(firstNonBlank(r.getServiceType(), q.getServiceType(), "ERP"));
        q.setSubject(r.getSubject());
        // Amount/currency remain backward-compatible in the API but are optional and are no longer required by the UI.
        if (r.getAmount() != null) q.setAmount(r.getAmount());
        if (r.getCurrency() != null && !r.getCurrency().isBlank()) q.setCurrency(r.getCurrency());
        q.setValidTill(r.getValidTill());
        if (r.getStatus() != null && !r.getStatus().isBlank()) q.setStatus(r.getStatus());
        q.setNotes(r.getNotes());

        // Merge: start from auto-mapped values (lead + quotation fields),
        // then let anything explicitly supplied in the request win — this is
        // what lets the telecaller manually edit an auto-filled value.
        Map<String, String> autoMapped = new LinkedHashMap<>();
        for (TemplateField field : template.getFields()) {
            String mapsTo = field.getMapsTo();
            if (mapsTo == null) continue;
            String value = switch (mapsTo) {
                case "clientName" -> q.getClientName();
                case "organization" -> q.getOrganization();
                case "email" -> q.getEmail();
                case "phone" -> q.getPhone();
                case "subject" -> r.getSubject();
                case "serviceType" -> q.getServiceType();
                case "address" -> q.getAddress();
                case "amount" -> r.getAmount() != null ? (q.getCurrency() + " " + r.getAmount()) : null;
                case "validTill" -> r.getValidTill();
                case "notes" -> r.getNotes();
                case "leadId" -> lead.getLeadId();
                case "date" -> DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault()).format(Instant.now());
                default -> null;
            };
            if (value != null) autoMapped.put(field.getId(), value);
        }
        if (r.getFieldValues() != null) autoMapped.putAll(r.getFieldValues());
        q.setFieldValues(autoMapped);
    }

    /** ICTPL/QTN/<year>/<3-digit running number>, unique even under concurrent requests or deletes. */
    private String nextQuotationNo() {
        int year = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).getYear();
        String key = "quotation-" + year;
        Query byKey = Query.query(Criteria.where("_id").is(key));
        try {
            mongo.upsert(byKey, new Update().setOnInsert("seq", numberStart - 1), "counters");
        } catch (DuplicateKeyException ignored) {
            // another request created the counter first - fine
        }
        Document doc = mongo.findAndModify(byKey, new Update().inc("seq", 1),
                FindAndModifyOptions.options().returnNew(true), Document.class, "counters");
        int seq = doc == null ? numberStart : ((Number) doc.get("seq")).intValue();
        return String.format("ICTPL/QTN/%d/%03d", year, seq);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private void addTimeline(Quotation q, String action, String icon, String bg, String color) {
        String time = DateTimeFormatter.ofPattern("dd MMM yyyy \u00b7 h:mm a").withZone(ZoneId.systemDefault()).format(Instant.now());
        q.getTimeline().add(new Quotation.TimelineEntry(action, time, icon, bg, color));
    }

    private String statusIcon(String status) {
        return switch (status) {
            case "Approved" -> "check";
            case "Rejected" -> "x";
            case "Revision Required" -> "edit";
            case "Viewed" -> "eye";
            case "Pending Approval" -> "clock";
            case "Sent" -> "send";
            default -> "info";
        };
    }

    private void checkOwner(Quotation q) {
        if (!access.currentUsername().equals(q.getCreatedBy())) {
            throw new AccessDeniedException("Quotation access denied");
        }
    }

    /** Draft, Rejected and Revision Required quotations may be edited; anything
     *  already in the approval pipeline (Pending Approval, Approved, Sent) is
     *  locked so a telecaller can't silently change what the Super Admin
     *  already reviewed. */
    private void requireEditable(Quotation q) {
        String status = q.getStatus();
        if (STATUS_PENDING_APPROVAL.equalsIgnoreCase(status) || STATUS_APPROVED.equalsIgnoreCase(status)
                || STATUS_SENT.equalsIgnoreCase(status)) {
            throw new IllegalStateException("This quotation is " + status + " and can no longer be edited.");
        }
    }

    private String currentTelecallerDisplayName(String username) {
        try {
            Telecaller tc = access.currentTelecaller();
            return tc.getFullName() != null && !tc.getFullName().isBlank() ? tc.getFullName() : username;
        } catch (Exception e) {
            return username;
        }
    }

    private String currentUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}