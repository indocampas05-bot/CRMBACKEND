package com.indocrm.indocrm.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attendance_audit_logs")
public class AttendanceAuditLog {
    @Id private String id;
    private String attendanceId;
    private String employeeId;
    private String username;
    private String field;
    private String previousValue;
    private String newValue;
    private String changedBy;
    private Instant changedAt;
    private String reason;

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getAttendanceId(){return attendanceId;} public void setAttendanceId(String v){attendanceId=v;}
    public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getField(){return field;} public void setField(String v){field=v;}
    public String getPreviousValue(){return previousValue;} public void setPreviousValue(String v){previousValue=v;}
    public String getNewValue(){return newValue;} public void setNewValue(String v){newValue=v;}
    public String getChangedBy(){return changedBy;} public void setChangedBy(String v){changedBy=v;}
    public Instant getChangedAt(){return changedAt;} public void setChangedAt(Instant v){changedAt=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
}
