package com.indocrm.indocrm.dto;

import jakarta.validation.constraints.NotBlank;

public class DemoRequest {
    @NotBlank private String leadId;
    @NotBlank private String topic;
    @NotBlank private String date;
    @NotBlank private String time;
    private String duration = "60 min";
    private String meetType = "meet";
    private String meetLink;
    private String status = "Scheduled";
    private String notes;

    public String getLeadId(){return leadId;} public void setLeadId(String v){leadId=v;}
    public String getTopic(){return topic;} public void setTopic(String v){topic=v;}
    public String getDate(){return date;} public void setDate(String v){date=v;}
    public String getTime(){return time;} public void setTime(String v){time=v;}
    public String getDuration(){return duration;} public void setDuration(String v){duration=v;}
    public String getMeetType(){return meetType;} public void setMeetType(String v){meetType=v;}
    public String getMeetLink(){return meetLink;} public void setMeetLink(String v){meetLink=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
