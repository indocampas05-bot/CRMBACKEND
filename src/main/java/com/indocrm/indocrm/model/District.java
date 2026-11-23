package com.indocrm.indocrm.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "districts")
public class District {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    
    private String assignedTo;
    private String assignedByName;
    private String assignedToId;
    
    private List<String> leadIds = new ArrayList<>();
    private int leadCount;
    
    private Date assignedDate;
    private Date createdAt;
    private Date updatedAt;

    // Default constructor
    public District() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }
    public String getAssignedToId() { return assignedToId; }
    public void setAssignedToId(String assignedToId) { this.assignedToId = assignedToId; }
    public List<String> getLeadIds() { return leadIds; }
    public void setLeadIds(List<String> leadIds) { this.leadIds = leadIds; }
    public int getLeadCount() { return leadCount; }
    public void setLeadCount(int leadCount) { this.leadCount = leadCount; }
    public Date getAssignedDate() { return assignedDate; }
    public void setAssignedDate(Date assignedDate) { this.assignedDate = assignedDate; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
