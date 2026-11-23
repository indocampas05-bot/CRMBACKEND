// FILE: src/main/java/com/indocrm/indocrm/model/Agreement.java
// REPLACES existing file — adds pincode, plan/pricing fields, signatory fields, generated-PDF fields.
// Everything that existed before is unchanged.
package com.indocrm.indocrm.model;
import java.time.Instant; import java.util.*;
import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection="agreements")
public class Agreement {
 @Id private String id;
 private String leadId,quotationId,templateId,serviceType,clientName,organization,email,phone,address,title,agreementType,contentHtml,headerHtml,footerHtml,logoDataUrl,letterPadDataUrl,createdBy,createdByName,approvedBy,rejectedBy,rejectionReason,approvalRemark,sentBy,sentChannel,sentTo;
 private AgreementStatus status=AgreementStatus.DRAFT; private int version=1;
 private Instant createdAt,updatedAt,submittedAt,approvedAt,rejectedAt,sentAt;
 private Map<String,String> mergeValues=new LinkedHashMap<>(); private List<AgreementHistory> history=new ArrayList<>();

 // NEW: client / editable-field data captured on the Add Agreement form
 private String pincode;
 private String agreementDate;               // yyyy-MM-dd, defaults to today if not supplied
 private String authorizedPersonName;
 private String authorizedPersonDesignation;

 // NEW: plan/pricing — resolved from the selected AgreementPlan at save time,
 // never hard-coded. Stored on the Agreement itself so history/approval
 // review shows exactly what was billed even if the plan is edited later.
 private String planId;
 private String planName;
 private Integer studentStrength;    // ERP only
 private double feeAmount;           // yearly fee (DM) / subscription fee (ERP)
 private double discountAmount;      // DM only
 private double gstPercent;
 private double gstAmount;           // ERP only (shown as its own line item)
 private double netPayable;          // DM: feeAmount - discountAmount
 private double totalPayable;        // ERP: feeAmount + gstAmount

 // NEW: generated PDF, produced by AgreementPdfService
 private String generatedPdfPath;
 private String generatedPdfFileName;

 public String getId(){return id;} public void setId(String v){id=v;} public String getLeadId(){return leadId;} public void setLeadId(String v){leadId=v;}
 public String getQuotationId(){return quotationId;} public void setQuotationId(String v){quotationId=v;}
 public String getTemplateId(){return templateId;} public void setTemplateId(String v){templateId=v;}
 public String getServiceType(){return serviceType;} public void setServiceType(String v){serviceType=v;}
 public String getClientName(){return clientName;} public void setClientName(String v){clientName=v;} public String getOrganization(){return organization;} public void setOrganization(String v){organization=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getAddress(){return address;} public void setAddress(String v){address=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getAgreementType(){return agreementType;} public void setAgreementType(String v){agreementType=v;}
 public String getContentHtml(){return contentHtml;} public void setContentHtml(String v){contentHtml=v;} public String getHeaderHtml(){return headerHtml;} public void setHeaderHtml(String v){headerHtml=v;}
 public String getFooterHtml(){return footerHtml;} public void setFooterHtml(String v){footerHtml=v;} public String getLogoDataUrl(){return logoDataUrl;} public void setLogoDataUrl(String v){logoDataUrl=v;}
 public String getLetterPadDataUrl(){return letterPadDataUrl;} public void setLetterPadDataUrl(String v){letterPadDataUrl=v;}
 public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;} public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;}
 public String getApprovedBy(){return approvedBy;} public void setApprovedBy(String v){approvedBy=v;} public String getRejectedBy(){return rejectedBy;} public void setRejectedBy(String v){rejectedBy=v;}
 public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;} public String getApprovalRemark(){return approvalRemark;} public void setApprovalRemark(String v){approvalRemark=v;}
 public String getSentBy(){return sentBy;} public void setSentBy(String v){sentBy=v;} public String getSentChannel(){return sentChannel;} public void setSentChannel(String v){sentChannel=v;} public String getSentTo(){return sentTo;} public void setSentTo(String v){sentTo=v;}
 public AgreementStatus getStatus(){return status;} public void setStatus(AgreementStatus v){status=v;} public int getVersion(){return version;} public void setVersion(int v){version=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
 public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;} public Instant getApprovedAt(){return approvedAt;} public void setApprovedAt(Instant v){approvedAt=v;}
 public Instant getRejectedAt(){return rejectedAt;} public void setRejectedAt(Instant v){rejectedAt=v;} public Instant getSentAt(){return sentAt;} public void setSentAt(Instant v){sentAt=v;}
 public Map<String,String> getMergeValues(){return mergeValues;} public void setMergeValues(Map<String,String> v){mergeValues=v==null?new LinkedHashMap<>():v;}
 public List<AgreementHistory> getHistory(){return history;} public void setHistory(List<AgreementHistory> v){history=v==null?new ArrayList<>():v;}

 public String getPincode(){return pincode;} public void setPincode(String v){pincode=v;}
 public String getAgreementDate(){return agreementDate;} public void setAgreementDate(String v){agreementDate=v;}
 public String getAuthorizedPersonName(){return authorizedPersonName;} public void setAuthorizedPersonName(String v){authorizedPersonName=v;}
 public String getAuthorizedPersonDesignation(){return authorizedPersonDesignation;} public void setAuthorizedPersonDesignation(String v){authorizedPersonDesignation=v;}
 public String getPlanId(){return planId;} public void setPlanId(String v){planId=v;}
 public String getPlanName(){return planName;} public void setPlanName(String v){planName=v;}
 public Integer getStudentStrength(){return studentStrength;} public void setStudentStrength(Integer v){studentStrength=v;}
 public double getFeeAmount(){return feeAmount;} public void setFeeAmount(double v){feeAmount=v;}
 public double getDiscountAmount(){return discountAmount;} public void setDiscountAmount(double v){discountAmount=v;}
 public double getGstPercent(){return gstPercent;} public void setGstPercent(double v){gstPercent=v;}
 public double getGstAmount(){return gstAmount;} public void setGstAmount(double v){gstAmount=v;}
 public double getNetPayable(){return netPayable;} public void setNetPayable(double v){netPayable=v;}
 public double getTotalPayable(){return totalPayable;} public void setTotalPayable(double v){totalPayable=v;}
 public String getGeneratedPdfPath(){return generatedPdfPath;} public void setGeneratedPdfPath(String v){generatedPdfPath=v;}
 public String getGeneratedPdfFileName(){return generatedPdfFileName;} public void setGeneratedPdfFileName(String v){generatedPdfFileName=v;}
}
