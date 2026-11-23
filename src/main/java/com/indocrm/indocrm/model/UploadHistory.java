package com.indocrm.indocrm.model;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "upload_history")
public class UploadHistory {
    
    @Id
    private String id;
    
    private String fileName;
    private String uploadedBy;
    private String district;
    private int totalRecords;
    private int importedRecords;
    private String status;
    private List<String> errors;
    private Date uploadedAt;

    // Default constructor
    public UploadHistory() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public int getImportedRecords() { return importedRecords; }
    public void setImportedRecords(int importedRecords) { this.importedRecords = importedRecords; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }
}
