package com.indocrm.indocrm.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payment_qr_codes")
public class PaymentQrCode {
    @Id private String id;
    private String fileName;
    private String contentType;
    private byte[] data;
    private Instant uploadedAt;
    private String uploadedBy;
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
    public byte[] getData(){return data;} public void setData(byte[] v){data=v;}
    public Instant getUploadedAt(){return uploadedAt;} public void setUploadedAt(Instant v){uploadedAt=v;}
    public String getUploadedBy(){return uploadedBy;} public void setUploadedBy(String v){uploadedBy=v;}
}

