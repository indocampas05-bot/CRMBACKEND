package com.indocrm.indocrm.dto;

import jakarta.validation.constraints.NotBlank;

public class QuotationTemplateRequest {
    @NotBlank private String name;
    private String fileName;
    private String mimeType = "text/html";
    @NotBlank private String html;

    public String getName(){return name;} public void setName(String v){name=v;}
    public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;}
    public String getHtml(){return html;} public void setHtml(String v){html=v;}
}
