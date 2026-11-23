package com.indocrm.indocrm.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "attendance_sessions")
public class AttendanceSession {
    @Id private String id;
    private String employeeId;
    private String username;
    private String attendanceId;
    private Instant loginAt;
    private Instant logoutAt;
    private Instant disconnectAt;
    private Instant reconnectAt;
    private Instant lastHeartbeatAt;
    private Instant lastActiveAt;
    private long activeSeconds;
    private long disconnectSeconds;
    private boolean connected;
    private boolean manuallyLoggedOut;
    private String deviceInfo;
    private String ipAddress;

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getEmployeeId(){return employeeId;} public void setEmployeeId(String v){employeeId=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getAttendanceId(){return attendanceId;} public void setAttendanceId(String v){attendanceId=v;}
    public Instant getLoginAt(){return loginAt;} public void setLoginAt(Instant v){loginAt=v;}
    public Instant getLogoutAt(){return logoutAt;} public void setLogoutAt(Instant v){logoutAt=v;}
    public Instant getDisconnectAt(){return disconnectAt;} public void setDisconnectAt(Instant v){disconnectAt=v;}
    public Instant getReconnectAt(){return reconnectAt;} public void setReconnectAt(Instant v){reconnectAt=v;}
    public Instant getLastHeartbeatAt(){return lastHeartbeatAt;} public void setLastHeartbeatAt(Instant v){lastHeartbeatAt=v;}
    public Instant getLastActiveAt(){return lastActiveAt;} public void setLastActiveAt(Instant v){lastActiveAt=v;}
    public long getActiveSeconds(){return activeSeconds;} public void setActiveSeconds(long v){activeSeconds=v;}
    public long getDisconnectSeconds(){return disconnectSeconds;} public void setDisconnectSeconds(long v){disconnectSeconds=v;}
    public boolean isConnected(){return connected;} public void setConnected(boolean v){connected=v;}
    public boolean isManuallyLoggedOut(){return manuallyLoggedOut;} public void setManuallyLoggedOut(boolean v){manuallyLoggedOut=v;}
    public String getDeviceInfo(){return deviceInfo;} public void setDeviceInfo(String v){deviceInfo=v;}
    public String getIpAddress(){return ipAddress;} public void setIpAddress(String v){ipAddress=v;}
}
