package com.indocrm.indocrm.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "payments")
public class Payment {

    // ============================================================
    // MAIN WORKFLOW STATUS
    // ============================================================

    public static final String STATUS_RECORDED = "Recorded";
    public static final String STATUS_VERIFIED = "Verified";
    public static final String STATUS_INVOICE_GENERATED = "Invoice Generated";
    public static final String STATUS_INVOICE_SENT_TO_TELECALLER =
            "Invoice Sent to Telecaller";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_FULLY_PAID = "Fully Paid";

    // ============================================================
    // PAYMENT TYPES
    // ============================================================

    public static final String TYPE_FULL = "Full";
    public static final String TYPE_PARTIAL = "Partial";
    public static final String TYPE_ADVANCE = "Advance";

    // ============================================================
    // PAYMENT IDENTITY
    // ============================================================

    @Id
    private String id;

    private String leadId;

    // ============================================================
    // CLIENT DETAILS
    // ============================================================

    private String organizationName;
    private String address;
    private String phone;
    private String email;

    // ============================================================
    // PAYMENT DETAILS
    // ============================================================

    private String paymentType = TYPE_FULL;

    /**
     * Total amount agreed for this client.
     */
    private Double totalAmount = 0.0;

    /**
     * Sum of all payment history records.
     */
    private Double amountPaid = 0.0;

    /**
     * totalAmount - amountPaid
     */
    private Double balanceAmount = 0.0;

    private String paymentDate;
    private String notes;

    // ============================================================
    // PAYMENT HISTORY
    // ============================================================

    private List<PaymentRecord> paymentHistory = new ArrayList<>();

    // ============================================================
    // MAIN PAYMENT STATUS
    // ============================================================

    /**
     * Recorded / Verified / Invoice Generated /
     * Invoice Sent to Telecaller / Completed / Fully Paid
     */
    private String status = STATUS_RECORDED;

    /**
     * Pending / Verified
     */
    private String verificationStatus = "Pending";

    /**
     * Not Generated / Draft / Finalized /
     * Sent to Telecaller / Sent to Client
     */
    private String invoiceStatus = "Not Generated";

    // ============================================================
    // OWNERSHIP
    // ============================================================

    private String createdBy;

    private String telecallerName;

    // ============================================================
    // PAYMENT VERIFICATION
    // ============================================================

    private String verifiedBy;

    private Instant verifiedAt;

    // ============================================================
    // PAYMENT REQUEST SEND DETAILS
    // ============================================================

    private String lastSentChannel;

    private Instant lastSentAt;

    // ============================================================
    // INVOICE
    // ============================================================

    private String invoiceId;

    // ============================================================
    // TIMELINE
    // ============================================================

    private List<TimelineEntry> timeline = new ArrayList<>();

    // ============================================================
    // AUDIT FIELDS
    // ============================================================

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;

    // ============================================================
    // PAYMENT RECORD
    // ============================================================

    public static class PaymentRecord {

        private String id;

        private Double amount = 0.0;

        private String paymentType;

        private String paymentDate;

        private String paymentMethod;

        private String transactionReferenceNumber;

        private String notes;

        private String status = "Paid";

        private String recordedBy;

        private Instant recordedAt;

        // --------------------------------------------------------
        // ID
        // --------------------------------------------------------

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        // --------------------------------------------------------
        // AMOUNT
        // --------------------------------------------------------

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        // --------------------------------------------------------
        // PAYMENT TYPE
        // --------------------------------------------------------

        public String getPaymentType() {
            return paymentType;
        }

        public void setPaymentType(String paymentType) {
            this.paymentType = paymentType;
        }

        // --------------------------------------------------------
        // PAYMENT DATE
        // --------------------------------------------------------

        public String getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        // --------------------------------------------------------
        // PAYMENT METHOD
        // --------------------------------------------------------

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        // --------------------------------------------------------
        // TRANSACTION REFERENCE
        // --------------------------------------------------------

        public String getTransactionReferenceNumber() {
            return transactionReferenceNumber;
        }

        public void setTransactionReferenceNumber(
                String transactionReferenceNumber) {
            this.transactionReferenceNumber =
                    transactionReferenceNumber;
        }

        // --------------------------------------------------------
        // NOTES
        // --------------------------------------------------------

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        // --------------------------------------------------------
        // RECORD STATUS
        // --------------------------------------------------------

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        // --------------------------------------------------------
        // RECORDED BY
        // --------------------------------------------------------

        public String getRecordedBy() {
            return recordedBy;
        }

        public void setRecordedBy(String recordedBy) {
            this.recordedBy = recordedBy;
        }

        // --------------------------------------------------------
        // RECORDED AT
        // --------------------------------------------------------

        public Instant getRecordedAt() {
            return recordedAt;
        }

        public void setRecordedAt(Instant recordedAt) {
            this.recordedAt = recordedAt;
        }
    }

    // ============================================================
    // TIMELINE ENTRY
    // ============================================================

    public static class TimelineEntry {

        private String action;

        private String time;

        private String icon;

        private String bg;

        private String color;

        public TimelineEntry() {
        }

        public TimelineEntry(
                String action,
                String time,
                String icon,
                String bg,
                String color
        ) {
            this.action = action;
            this.time = time;
            this.icon = icon;
            this.bg = bg;
            this.color = color;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getBg() {
            return bg;
        }

        public void setBg(String bg) {
            this.bg = bg;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    // ============================================================
    // MAIN PAYMENT GETTERS AND SETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Double getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(Double balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PaymentRecord> getPaymentHistory() {
        return paymentHistory;
    }

    public void setPaymentHistory(
            List<PaymentRecord> paymentHistory) {

        this.paymentHistory =
                paymentHistory == null
                        ? new ArrayList<>()
                        : paymentHistory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ============================================================
    // VERIFICATION STATUS
    // ============================================================

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(
            String verificationStatus) {

        this.verificationStatus = verificationStatus;
    }

    // ============================================================
    // INVOICE STATUS
    // ============================================================

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(
            String invoiceStatus) {

        this.invoiceStatus = invoiceStatus;
    }

    // ============================================================
    // OWNERSHIP
    // ============================================================

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTelecallerName() {
        return telecallerName;
    }

    public void setTelecallerName(String telecallerName) {
        this.telecallerName = telecallerName;
    }

    // ============================================================
    // VERIFICATION DETAILS
    // ============================================================

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    // ============================================================
    // PAYMENT REQUEST SEND DETAILS
    // ============================================================

    public String getLastSentChannel() {
        return lastSentChannel;
    }

    public void setLastSentChannel(
            String lastSentChannel) {

        this.lastSentChannel = lastSentChannel;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    // ============================================================
    // INVOICE
    // ============================================================

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    // ============================================================
    // TIMELINE
    // ============================================================

    public List<TimelineEntry> getTimeline() {
        return timeline;
    }

    public void setTimeline(
            List<TimelineEntry> timeline) {

        this.timeline =
                timeline == null
                        ? new ArrayList<>()
                        : timeline;
    }

    // ============================================================
    // AUDIT
    // ============================================================

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}