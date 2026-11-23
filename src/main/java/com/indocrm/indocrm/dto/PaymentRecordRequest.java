package com.indocrm.indocrm.dto;
public class PaymentRecordRequest {
 private Double paymentAmount; private String paymentType; private String paymentDate; private String paymentMethod; private String transactionReferenceNumber; private String notes; private String status;
 public Double getPaymentAmount(){return paymentAmount;} public void setPaymentAmount(Double v){paymentAmount=v;}
 public String getPaymentType(){return paymentType;} public void setPaymentType(String v){paymentType=v;}
 public String getPaymentDate(){return paymentDate;} public void setPaymentDate(String v){paymentDate=v;}
 public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
 public String getTransactionReferenceNumber(){return transactionReferenceNumber;} public void setTransactionReferenceNumber(String v){transactionReferenceNumber=v;}
 public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
}

