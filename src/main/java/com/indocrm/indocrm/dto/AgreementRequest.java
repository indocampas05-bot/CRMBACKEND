// FILE: src/main/java/com/indocrm/indocrm/dto/AgreementRequest.java
// REPLACES existing file — adds pincode, planId, studentStrength, authorized-signatory
// fields and an optional discount override.
package com.indocrm.indocrm.dto;
import java.util.*;
public class AgreementRequest {
    private String leadId, quotationId, templateId, serviceType, title, agreementType,
            contentHtml, headerHtml, footerHtml, logoDataUrl, letterPadDataUrl;
    private Map<String,String> mergeValues = new LinkedHashMap<>();

    // NEW
    private String pincode;
    private String agreementDate;              // optional, yyyy-MM-dd; defaults to today
    private String authorizedPersonName;
    private String authorizedPersonDesignation;
    private String planId;                     // required — selects the AgreementPlan
    private Integer studentStrength;            // ERP only
    private Double discountOverride;            // DM only, optional — overrides the plan's default discount

    public String getLeadId(){return leadId;} public void setLeadId(String v){leadId=v;}
    public String getQuotationId(){return quotationId;} public void setQuotationId(String v){quotationId=v;}
    public String getTemplateId(){return templateId;} public void setTemplateId(String v){templateId=v;}
    public String getServiceType(){return serviceType;} public void setServiceType(String v){serviceType=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getAgreementType(){return agreementType;} public void setAgreementType(String v){agreementType=v;}
    public String getContentHtml(){return contentHtml;} public void setContentHtml(String v){contentHtml=v;}
    public String getHeaderHtml(){return headerHtml;} public void setHeaderHtml(String v){headerHtml=v;}
    public String getFooterHtml(){return footerHtml;} public void setFooterHtml(String v){footerHtml=v;}
    public String getLogoDataUrl(){return logoDataUrl;} public void setLogoDataUrl(String v){logoDataUrl=v;}
    public String getLetterPadDataUrl(){return letterPadDataUrl;} public void setLetterPadDataUrl(String v){letterPadDataUrl=v;}
    public Map<String,String> getMergeValues(){return mergeValues;} public void setMergeValues(Map<String,String> v){mergeValues=v==null?new LinkedHashMap<>():v;}

    public String getPincode(){return pincode;} public void setPincode(String v){pincode=v;}
    public String getAgreementDate(){return agreementDate;} public void setAgreementDate(String v){agreementDate=v;}
    public String getAuthorizedPersonName(){return authorizedPersonName;} public void setAuthorizedPersonName(String v){authorizedPersonName=v;}
    public String getAuthorizedPersonDesignation(){return authorizedPersonDesignation;} public void setAuthorizedPersonDesignation(String v){authorizedPersonDesignation=v;}
    public String getPlanId(){return planId;} public void setPlanId(String v){planId=v;}
    public Integer getStudentStrength(){return studentStrength;} public void setStudentStrength(Integer v){studentStrength=v;}
    public Double getDiscountOverride(){return discountOverride;} public void setDiscountOverride(Double v){discountOverride=v;}
}
