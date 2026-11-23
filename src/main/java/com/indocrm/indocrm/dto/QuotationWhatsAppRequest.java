package com.indocrm.indocrm.dto;

import jakarta.validation.constraints.NotBlank;

public class QuotationWhatsAppRequest {
    @NotBlank private String phone;
    private String message;

    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
}
