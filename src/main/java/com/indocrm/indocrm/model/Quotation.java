package com.indocrm.indocrm.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "quotations")
public class Quotation {
    private String id;
    private String quotationNo;            // e.g. ICTPL/QTN/2026/073
    private String leadId;
    private String templateId;

    private String clientName;
    private String organization;
    private String email;
    private String phone;

    private String subject;
    private String serviceType;
    private String address;
    private Double amount;
    private String currency = "\u20B9";
    private String validTill;
    private String status = "Draft";
    private String notes;

    // Values the telecaller typed into each template field, keyed by
    // TemplateField.id. Auto-filled from the lead where a field's `mapsTo`
    // matches, then freely editable before generating the PDF.
    private Map<String, String> fieldValues = new LinkedHashMap<>();

    private String generatedPdfPath;   // absolute path on disk
    private String generatedPdfFileName;

    private String createdBy;              // telecaller username who owns this quotation

    // ── Approval workflow ────────────────────────────────────────────────
    // status now also carries: "Draft", "Pending Approval", "Approved",
    // "Rejected", "Revision Required", "Sent" (in addition to any legacy
    // values already stored).
    private String telecallerName;         // display name, snapshotted at submit time
    private Instant submittedAt;           // when the telecaller submitted for approval
    private Instant reviewedAt;            // when the super admin approved/rejected
    private String reviewedBy;             // super admin username who reviewed
    private String rejectionReason;        // set when status == Rejected
    private String revisionNotes;          // set when status == Revision Required (Super Admin's requested changes)
    private Instant sentAt;                // when the telecaller sent the approved quotation to the lead
    private String sentChannel;            // "whatsapp" | "email" | "message" etc.

    private List<TimelineEntry> timeline = new ArrayList<>();

    @CreatedDate @Field("createdAt") private Instant createdAt;
    @LastModifiedDate @Field("updatedAt") private Instant updatedAt;

    public static class TimelineEntry {
        private String action, time, icon, bg, color;
        public TimelineEntry() {}
        public TimelineEntry(String action, String time, String icon, String bg, String color) { this.action=action;this.time=time;this.icon=icon;this.bg=bg;this.color=color; }
        public String getAction(){return action;} public void setAction(String v){action=v;}
        public String getTime(){return time;} public void setTime(String v){time=v;}
        public String getIcon(){return icon;} public void setIcon(String v){icon=v;}
        public String getBg(){return bg;} public void setBg(String v){bg=v;}
        public String getColor(){return color;} public void setColor(String v){color=v;}
    }

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getQuotationNo(){return quotationNo;} public void setQuotationNo(String v){quotationNo=v;}
    public String getLeadId(){return leadId;} public void setLeadId(String v){leadId=v;}
    public String getTemplateId(){return templateId;} public void setTemplateId(String v){templateId=v;}
    public String getClientName(){return clientName;} public void setClientName(String v){clientName=v;}
    public String getOrganization(){return organization;} public void setOrganization(String v){organization=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getSubject(){return subject;} public void setSubject(String v){subject=v;}
    public String getServiceType(){return serviceType;} public void setServiceType(String v){serviceType=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public Double getAmount(){return amount;} public void setAmount(Double v){amount=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
    public String getValidTill(){return validTill;} public void setValidTill(String v){validTill=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public Map<String, String> getFieldValues(){return fieldValues;} public void setFieldValues(Map<String, String> v){fieldValues=v;}
    public String getGeneratedPdfPath(){return generatedPdfPath;} public void setGeneratedPdfPath(String v){generatedPdfPath=v;}
    public String getGeneratedPdfFileName(){return generatedPdfFileName;} public void setGeneratedPdfFileName(String v){generatedPdfFileName=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public String getTelecallerName(){return telecallerName;} public void setTelecallerName(String v){telecallerName=v;}
    public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;}
    public Instant getReviewedAt(){return reviewedAt;} public void setReviewedAt(Instant v){reviewedAt=v;}
    public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String v){reviewedBy=v;}
    public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;}
    public String getRevisionNotes(){return revisionNotes;} public void setRevisionNotes(String v){revisionNotes=v;}
    public Instant getSentAt(){return sentAt;} public void setSentAt(Instant v){sentAt=v;}
    public String getSentChannel(){return sentChannel;} public void setSentChannel(String v){sentChannel=v;}
    public List<TimelineEntry> getTimeline(){return timeline;} public void setTimeline(List<TimelineEntry> v){timeline=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}