package com.indocrm.indocrm.model;

import java.util.Date;
import org.springframework.data.mongodb.core.mapping.Field;

public class LeadActivity {
    private String id;
    private String type;
    private String source;
    private String action;
    private String field;
    @Field("previousValue")
    private String previousValue;
    @Field("newValue")
    private String newValue;
    private String description;
    private String updatedBy;
    private Date createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
