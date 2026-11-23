package com.indocrm.indocrm.dto;

public class PaymentRequest {
    private String leadId;
    private String organizationName;
    private String address;
    private String phone;
    private String email;
    private String paymentType;   // Full | Partial | Advance
    private Double totalAmount;
    private Double amountPaid;
    private String paymentDate;
    private String notes;
    private String paymentMethod;
    private String transactionReferenceNumber;
    private String paymentStatus;

    public String getLeadId(){return leadId;} public void setLeadId(String v){leadId=v;}
    public String getOrganizationName(){return organizationName;} public void setOrganizationName(String v){organizationName=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPaymentType(){return paymentType;} public void setPaymentType(String v){paymentType=v;}
    public Double getTotalAmount(){return totalAmount;} public void setTotalAmount(Double v){totalAmount=v;}
    public Double getAmountPaid(){return amountPaid;} public void setAmountPaid(Double v){amountPaid=v;}
    public String getPaymentDate(){return paymentDate;} public void setPaymentDate(String v){paymentDate=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
    public String getTransactionReferenceNumber(){return transactionReferenceNumber;} public void setTransactionReferenceNumber(String v){transactionReferenceNumber=v;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
}

