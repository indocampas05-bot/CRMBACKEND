// BACKEND: src/main/java/com/indocrm/indocrm/model/CallAttempt.java

package com.indocrm.indocrm.model;

public class CallAttempt {
    private String number;
    private String status;
    private String time;
    private Integer duration;
    
    public CallAttempt() {}
    
    public CallAttempt(String number, String status, String time) {
        this.number = number;
        this.status = status;
        this.time = time;
    }
    
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
