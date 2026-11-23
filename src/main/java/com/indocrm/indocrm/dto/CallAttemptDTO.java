// FILE: src/main/java/com/indocrm/indocrm/dto/CallAttemptDTO.java

package com.indocrm.indocrm.dto;

public class CallAttemptDTO {
    private String number;
    private String status;
    private String time;
    private Integer duration;
    
    public CallAttemptDTO() {}
    
    // Getters and Setters
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
