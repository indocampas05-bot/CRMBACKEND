package com.indocrm.indocrm.dto;

import java.util.Map;

public class QuotationRequest {
    private String leadId;
    private String templateId;
    private String subject;
    private String serviceType;
    private String clientName;
    private String organization;
    private String email;
    private String phone;
    private String address;
    private Double amount;
    private String currency;
    private String validTill;
    private String status;
    private String notes;
    private Map<String, String> fieldValues;

    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getValidTill() { return validTill; }
    public void setValidTill(String validTill) { this.validTill = validTill; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Map<String, String> getFieldValues() { return fieldValues; }
    public void setFieldValues(Map<String, String> fieldValues) { this.fieldValues = fieldValues; }
}
