// FILE: src/main/java/com/indocrm/indocrm/model/AgreementTemplate.java
// REPLACES existing file — adds agreementType + templateVersion, everything else unchanged
package com.indocrm.indocrm.model;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Document(collection="agreement_templates")
public class AgreementTemplate {
 @Id private String id;
 private String name,fileName,contentType,sourceType,serviceType,contentHtml,backgroundDataUrl,createdBy;
 private boolean systemTemplate,active=true;
 private byte[] originalFile;
 private Instant createdAt,updatedAt;

 // NEW: which Add-Agreement flow this template belongs to — "ERP" or
 // "DIGITAL_MARKETING". Drives which form fields/plans are shown and lets
 // AgreementService validate the picked plan matches the picked template.
 private String agreementType;

 // NEW: bumped whenever the *hard-coded* system template content changes
 // (see AgreementTemplateBootstrapService). Lets the bootstrap self-heal a
 // system template's contentHtml on the next deploy without clobbering a
 // template an admin has since customized by hand (systemTemplate=false or
 // templateVersion tampered with directly means "leave it alone").
 private int templateVersion;
 private String category;

 public String getId(){return id;} public void setId(String v){id=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
 public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
 public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
 public String getServiceType(){return serviceType;} public void setServiceType(String v){serviceType=v;}
 public String getContentHtml(){return contentHtml;} public void setContentHtml(String v){contentHtml=v;}
 public String getBackgroundDataUrl(){return backgroundDataUrl;} public void setBackgroundDataUrl(String v){backgroundDataUrl=v;}
 public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
 public String getCategory(){return category;} public void setCategory(String v){category=v;}
 public boolean isSystemTemplate(){return systemTemplate;} public void setSystemTemplate(boolean v){systemTemplate=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public byte[] getOriginalFile(){return originalFile;} public void setOriginalFile(byte[] v){originalFile=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
 public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
 public String getAgreementType(){return agreementType;} public void setAgreementType(String v){agreementType=v;}
 public int getTemplateVersion(){return templateVersion;} public void setTemplateVersion(int v){templateVersion=v;}
}
