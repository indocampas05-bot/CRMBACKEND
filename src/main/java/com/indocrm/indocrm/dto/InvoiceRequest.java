package com.indocrm.indocrm.dto;

import java.util.List;
import com.indocrm.indocrm.model.Payment;

/** Editable Super Admin invoice draft/finalization payload. */
public class InvoiceRequest {
    private String organizationName, address, phone, email, paymentType, paymentDate, paymentStatus, notes;
    private Double totalAmount, amountPaid, balanceAmount;
    private List<Payment.PaymentRecord> paymentHistory;
    public String getOrganizationName(){return organizationName;} public void setOrganizationName(String v){organizationName=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPaymentType(){return paymentType;} public void setPaymentType(String v){paymentType=v;}
    public String getPaymentDate(){return paymentDate;} public void setPaymentDate(String v){paymentDate=v;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public Double getTotalAmount(){return totalAmount;} public void setTotalAmount(Double v){totalAmount=v;}
    public Double getAmountPaid(){return amountPaid;} public void setAmountPaid(Double v){amountPaid=v;}
    public Double getBalanceAmount(){return balanceAmount;} public void setBalanceAmount(Double v){balanceAmount=v;}
    public List<Payment.PaymentRecord> getPaymentHistory(){return paymentHistory;} public void setPaymentHistory(List<Payment.PaymentRecord> v){paymentHistory=v;}
}
