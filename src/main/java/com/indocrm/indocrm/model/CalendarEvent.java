package com.indocrm.indocrm.model;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "calendar_events")
public class CalendarEvent {
    @Id private String id;
    private LocalDate date;
    private String title;
    private String link;
    private String type; // MEETING, HOLIDAY, EVENT
    private List<String> assignedTo; // null/empty = visible to all authorized users
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
    private boolean active = true;
    public String getId(){return id;} public void setId(String id){this.id=id;}
    public LocalDate getDate(){return date;} public void setDate(LocalDate date){this.date=date;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getLink(){return link;} public void setLink(String link){this.link=link;}
    public String getType(){return type;} public void setType(String type){this.type=type;}
    public List<String> getAssignedTo(){return assignedTo;} public void setAssignedTo(List<String> assignedTo){this.assignedTo=assignedTo;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String createdBy){this.createdBy=createdBy;}
    public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date createdAt){this.createdAt=createdAt;}
    public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date updatedAt){this.updatedAt=updatedAt;}
    public boolean isActive(){return active;} public void setActive(boolean active){this.active=active;}
}
