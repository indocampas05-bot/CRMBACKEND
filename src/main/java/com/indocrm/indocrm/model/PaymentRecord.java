package com.indocrm.indocrm.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payment_records")
public class PaymentRecord {
    @Id private String id;
    private String leadId;
    private String organizationName;
    private String address;
    private String phoneNumber;
    private String email;
    private String telecallerName;
    private double totalAmount;
    private double amountPaid;
    private double balanceAmount;
    private String paymentType; // FULL, PARTIAL, ADVANCE
    private String paymentStatus; // PENDING, RECORDED, VERIFIED, REJECTED
    private String paymentDate;
    private String paymentMethod;
    private String remarks;
    private Instant createdAt;
    private Instant updatedAt;
    private String verifiedBy;
    private Instant verifiedAt;
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getLeadId(){return leadId;} public void setLeadId(String v){leadId=v;}
    public String getOrganizationName(){return organizationName;} public void setOrganizationName(String v){organizationName=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getTelecallerName(){return telecallerName;} public void setTelecallerName(String v){telecallerName=v;}
    public double getTotalAmount(){return totalAmount;} public void setTotalAmount(double v){totalAmount=v;}
    public double getAmountPaid(){return amountPaid;} public void setAmountPaid(double v){amountPaid=v;}
    public double getBalanceAmount(){return balanceAmount;} public void setBalanceAmount(double v){balanceAmount=v;}
    public String getPaymentType(){return paymentType;} public void setPaymentType(String v){paymentType=v;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
    public String getPaymentDate(){return paymentDate;} public void setPaymentDate(String v){paymentDate=v;}
    public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
    public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
    public String getVerifiedBy(){return verifiedBy;} public void setVerifiedBy(String v){verifiedBy=v;}
    public Instant getVerifiedAt(){return verifiedAt;} public void setVerifiedAt(Instant v){verifiedAt=v;}
}

