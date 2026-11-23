package com.indocrm.indocrm.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attendance_records")
@CompoundIndex(name = "uk_attendance_employee_date", def = "{'employeeId': 1, 'date': 1}", unique = true)
public class AttendanceRecord {
    @Id private String id;
    private String employeeId;
    private String username;
    private String fullName;
    private String date;
    private String status;
    private Instant firstLoginAt;
    private Instant lastLogoutAt;
    private String currentSessionId;
    private long totalWorkingSeconds;
    private Instant workingAccumulatedAt;
    private boolean manuallyOverridden;
    private String manualOverrideStatus;
    private String notes;
    private String remarks;
    private int sessionCount;
    private boolean online;
    private boolean disconnected;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> networkEvents = new ArrayList<>();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getDate(){return date;} public void setDate(String v){date=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Instant getFirstLoginAt(){return firstLoginAt;} public void setFirstLoginAt(Instant v){firstLoginAt=v;}
    public Instant getLastLogoutAt(){return lastLogoutAt;} public void setLastLogoutAt(Instant v){lastLogoutAt=v;}
    public String getCurrentSessionId(){return currentSessionId;} public void setCurrentSessionId(String v){currentSessionId=v;}
    public long getTotalWorkingSeconds(){return totalWorkingSeconds;} public void setTotalWorkingSeconds(long v){totalWorkingSeconds=v;}
    public Instant getWorkingAccumulatedAt(){return workingAccumulatedAt;} public void setWorkingAccumulatedAt(Instant v){workingAccumulatedAt=v;}
    public boolean isManuallyOverridden(){return manuallyOverridden;} public void setManuallyOverridden(boolean v){manuallyOverridden=v;}
    public String getManualOverrideStatus(){return manualOverrideStatus;} public void setManualOverrideStatus(String v){manualOverrideStatus=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
    public int getSessionCount(){return sessionCount;} public void setSessionCount(int v){sessionCount=v;}
    public boolean isOnline(){return online;} public void setOnline(boolean v){online=v;}
    public boolean isDisconnected(){return disconnected;} public void setDisconnected(boolean v){disconnected=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
    public List<String> getNetworkEvents(){return networkEvents;} public void setNetworkEvents(List<String> v){networkEvents=v;}
}
