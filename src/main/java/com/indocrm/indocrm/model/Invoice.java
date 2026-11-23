package com.indocrm.indocrm.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "invoices")
public class Invoice {

    @Id
    private String id;

    private String paymentId;
    private String leadId;
    private String invoiceNumber;

    // ============================================================
    // CLIENT DETAILS
    // ============================================================

    private String organizationName;
    private String address;
    private String phone;
    private String customerGstin;
    private String email;

    // ============================================================
    // PAYMENT SUMMARY
    // ============================================================

    private Double totalAmount = 0.0;
    private Double amountPaid = 0.0;
    private Double balanceAmount = 0.0;

    private String paymentType;
    private String paymentDate;

    // Verified / Fully Paid / Partial / Pending etc.
    private String paymentStatus;

    // ============================================================
    // PAYMENT HISTORY SNAPSHOT
    // ============================================================

    private List<Payment.PaymentRecord> paymentHistory =
            new ArrayList<>();

    // ============================================================
    // INVOICE GENERATION
    // ============================================================

    private String generatedBy;
    private Instant generatedAt;

    // ============================================================
    // SHARING STATUS
    // ============================================================

    private Instant sentToTelecallerAt;

    private Instant sentToClientAt;

    // WhatsApp | Email
    private String sentToClientChannel;

    // ============================================================
    // PDF FILE
    // ============================================================

    private String pdfPath;
    private String pdfFileName;

    // ============================================================
    // EXCEL FILE
    // ============================================================

    private String excelPath;
    private String excelFileName;

    // ============================================================
    // AUDIT
    // ============================================================

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;



    // ============================================================
    // EDITABLE INVOICE DETAILS (Super Admin can change these)
    // ============================================================

    // yyyy-MM-dd
    private String invoiceDate;

    // line item description shown on the invoice
    private String description;

    private Double discountAmount = 0.0;

    // percentage, default 18 (GST), editable
    private Double taxRate = 18.0;

    // ---- fully editable invoice content (null = use the default template text) ----
    private String shippingName;
    private String shippingAddress;
    private String companyName;
    private String companyAddress;
    private String companyEmail;
    private String companyMobile;
    private String companyWebsite;
    private String bankName;
    private String bankBranch;
    private String bankIfsc;
    private String bankAccountNo;
    private String declaration;
    private String signatoryFor;
    private String signatoryName;
    private String signatoryTitle;

    /** Editable line items. Invoice totalAmount = sum(quantity * rate). */
    private List<Item> items;

    public static class Item {
        private String description;
        private Double quantity = 1.0;
        private Double rate = 0.0;

        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double v) { quantity = v; }
        public Double getRate() { return rate; }
        public void setRate(Double v) { rate = v; }
    }

    private String editedBy;
    private Instant editedAt;

    // ============================================================
    // GETTERS AND SETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public void setId(String v) {
        id = v;
    }
public String getCustomerGstin() { return customerGstin; }
public void setCustomerGstin(String v) { customerGstin = v; }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String v) {
        paymentId = v;
    }


    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String v) {
        leadId = v;
    }


    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String v) {
        invoiceNumber = v;
    }


    // ============================================================
    // CLIENT DETAILS
    // ============================================================

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String v) {
        organizationName = v;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String v) {
        address = v;
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        phone = v;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        email = v;
    }


    // ============================================================
    // PAYMENT SUMMARY
    // ============================================================

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double v) {
        totalAmount = v;
    }


    public Double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double v) {
        amountPaid = v;
    }


    public Double getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(Double v) {
        balanceAmount = v;
    }


    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String v) {
        paymentType = v;
    }


    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String v) {
        paymentDate = v;
    }


    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String v) {
        paymentStatus = v;
    }


    // ============================================================
    // PAYMENT HISTORY
    // ============================================================

    public List<Payment.PaymentRecord> getPaymentHistory() {
        return paymentHistory;
    }

    public void setPaymentHistory(
            List<Payment.PaymentRecord> v
    ) {
        paymentHistory =
                v == null
                        ? new ArrayList<>()
                        : v;
    }


    // ============================================================
    // GENERATION
    // ============================================================

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String v) {
        generatedBy = v;
    }


    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant v) {
        generatedAt = v;
    }


    // ============================================================
    // TELECALLER
    // ============================================================

    public Instant getSentToTelecallerAt() {
        return sentToTelecallerAt;
    }

    public void setSentToTelecallerAt(Instant v) {
        sentToTelecallerAt = v;
    }


    // ============================================================
    // CLIENT
    // ============================================================

    public Instant getSentToClientAt() {
        return sentToClientAt;
    }

    public void setSentToClientAt(Instant v) {
        sentToClientAt = v;
    }


    public String getSentToClientChannel() {
        return sentToClientChannel;
    }

    public void setSentToClientChannel(String v) {
        sentToClientChannel = v;
    }


    // ============================================================
    // PDF
    // ============================================================

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String v) {
        pdfPath = v;
    }


    public String getPdfFileName() {
        return pdfFileName;
    }

    public void setPdfFileName(String v) {
        pdfFileName = v;
    }


    // ============================================================
    // EXCEL
    // ============================================================

    public String getExcelPath() {
        return excelPath;
    }

    public void setExcelPath(String v) {
        excelPath = v;
    }


    public String getExcelFileName() {
        return excelFileName;
    }

    public void setExcelFileName(String v) {
        excelFileName = v;
    }


    // ============================================================
    // AUDIT
    // ============================================================

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant v) {
        createdAt = v;
    }


    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant v) {
        updatedAt = v;
    }

    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String v) { invoiceDate = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double v) { discountAmount = v; }

    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double v) { taxRate = v; }

    public String getShippingName() { return shippingName; }
    public void setShippingName(String v) { shippingName = v; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String v) { shippingAddress = v; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String v) { companyName = v; }

    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String v) { companyAddress = v; }

    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String v) { companyEmail = v; }

    public String getCompanyMobile() { return companyMobile; }
    public void setCompanyMobile(String v) { companyMobile = v; }

    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String v) { companyWebsite = v; }

    public String getBankName() { return bankName; }
    public void setBankName(String v) { bankName = v; }

    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String v) { bankBranch = v; }

    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String v) { bankIfsc = v; }

    public String getBankAccountNo() { return bankAccountNo; }
    public void setBankAccountNo(String v) { bankAccountNo = v; }

    public String getDeclaration() { return declaration; }
    public void setDeclaration(String v) { declaration = v; }

    public String getSignatoryFor() { return signatoryFor; }
    public void setSignatoryFor(String v) { signatoryFor = v; }

    public String getSignatoryName() { return signatoryName; }
    public void setSignatoryName(String v) { signatoryName = v; }

    public String getSignatoryTitle() { return signatoryTitle; }
    public void setSignatoryTitle(String v) { signatoryTitle = v; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> v) { items = v; }

    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String v) { editedBy = v; }

    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant v) { editedAt = v; }
}