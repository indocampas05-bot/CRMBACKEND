package com.indocrm.indocrm.dto;

import java.util.List;

import com.indocrm.indocrm.model.Invoice;

/** Everything the Super Admin may edit on a generated invoice. null = leave unchanged. */
public class InvoiceUpdateRequest {
    private String invoiceNumber, invoiceDate, organizationName, address, phone, email, paymentType, paymentStatus, shippingName, shippingAddress, companyName, companyAddress, companyEmail, companyMobile, companyWebsite, bankName, bankBranch, bankIfsc, bankAccountNo, declaration, signatoryFor, signatoryName, signatoryTitle;
    private Double discountAmount, taxRate, amountPaid;
    private List<Invoice.Item> items;
    private String customerGstin;
public String getCustomerGstin(){return customerGstin;} public void setCustomerGstin(String v){customerGstin=v;}

    public String getInvoiceNumber(){return invoiceNumber;} public void setInvoiceNumber(String v){invoiceNumber=v;}
    public String getInvoiceDate(){return invoiceDate;} public void setInvoiceDate(String v){invoiceDate=v;}
    public String getOrganizationName(){return organizationName;} public void setOrganizationName(String v){organizationName=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPaymentType(){return paymentType;} public void setPaymentType(String v){paymentType=v;}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
    public String getShippingName(){return shippingName;} public void setShippingName(String v){shippingName=v;}
    public String getShippingAddress(){return shippingAddress;} public void setShippingAddress(String v){shippingAddress=v;}
    public String getCompanyName(){return companyName;} public void setCompanyName(String v){companyName=v;}
    public String getCompanyAddress(){return companyAddress;} public void setCompanyAddress(String v){companyAddress=v;}
    public String getCompanyEmail(){return companyEmail;} public void setCompanyEmail(String v){companyEmail=v;}
    public String getCompanyMobile(){return companyMobile;} public void setCompanyMobile(String v){companyMobile=v;}
    public String getCompanyWebsite(){return companyWebsite;} public void setCompanyWebsite(String v){companyWebsite=v;}
    public String getBankName(){return bankName;} public void setBankName(String v){bankName=v;}
    public String getBankBranch(){return bankBranch;} public void setBankBranch(String v){bankBranch=v;}
    public String getBankIfsc(){return bankIfsc;} public void setBankIfsc(String v){bankIfsc=v;}
    public String getBankAccountNo(){return bankAccountNo;} public void setBankAccountNo(String v){bankAccountNo=v;}
    public String getDeclaration(){return declaration;} public void setDeclaration(String v){declaration=v;}
    public String getSignatoryFor(){return signatoryFor;} public void setSignatoryFor(String v){signatoryFor=v;}
    public String getSignatoryName(){return signatoryName;} public void setSignatoryName(String v){signatoryName=v;}
    public String getSignatoryTitle(){return signatoryTitle;} public void setSignatoryTitle(String v){signatoryTitle=v;}
    public Double getDiscountAmount(){return discountAmount;} public void setDiscountAmount(Double v){discountAmount=v;}
    public Double getTaxRate(){return taxRate;} public void setTaxRate(Double v){taxRate=v;}
    public Double getAmountPaid(){return amountPaid;} public void setAmountPaid(Double v){amountPaid=v;}
    public List<Invoice.Item> getItems(){return items;} public void setItems(List<Invoice.Item> v){items=v;}
}