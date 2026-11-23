package com.indocrm.indocrm.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "demos")
public class Demo {
    private String id;
    private String organization;
    private String contactPerson;
    private String leadId;
    private String email;
    private String phone;
    private String topic;
    private String date;
    private String time;
    private String duration;
    private String meetType;
    private String meetLink;
    private String status;
    private String notes;
    private String createdBy;
    private List<TimelineEntry> timeline = new ArrayList<>();

    @CreatedDate @Field("createdAt") private Instant createdAt;
    @LastModifiedDate @Field("updatedAt") private Instant updatedAt;

    public static class TimelineEntry {
        private String action, time, icon, bg, color;
        public TimelineEntry() {}
        public TimelineEntry(String action, String time, String icon, String bg, String color) { this.action=action;this.time=time;this.icon=icon;this.bg=bg;this.color=color; }
        public String getAction(){return action;} public void setAction(String v){action=v;}
        public String getTime(){return time;} public void setTime(String v){time=v;}
        public String getIcon(){return icon;} public void setIcon(String v){icon=v;}
        public String getBg(){return bg;} public void setBg(String v){bg=v;}
        public String getColor(){return color;} public void setColor(String v){color=v;}
    }
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getOrganization(){return organization;} public void setOrganization(String v){organization=v;}
    public String getContactPerson(){return contactPerson;} public void setContactPerson(String v){contactPerson=v;}
    public String getLeadId(){return leadId;} public void setLeadId(String v){leadId=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getTopic(){return topic;} public void setTopic(String v){topic=v;}
    public String getDate(){return date;} public void setDate(String v){date=v;}
    public String getTime(){return time;} public void setTime(String v){time=v;}
    public String getDuration(){return duration;} public void setDuration(String v){duration=v;}
    public String getMeetType(){return meetType;} public void setMeetType(String v){meetType=v;}
    public String getMeetLink(){return meetLink;} public void setMeetLink(String v){meetLink=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public List<TimelineEntry> getTimeline(){return timeline;} public void setTimeline(List<TimelineEntry> v){timeline=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}

