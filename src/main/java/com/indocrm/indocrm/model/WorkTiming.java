// FILE: src/main/java/com/indocrm/indocrm/model/WorkTiming.java

package com.indocrm.indocrm.model;

public class WorkTiming {
    private String shiftName;
    private String shiftStart;
    private String shiftEnd;
    private int graceTime;
    private int workingHours;
    private boolean enableLunch;
    private String lunchStart;
    private String lunchEnd;
    private boolean enableTeaBreak;
    private String morningTeaStart;
    private String morningTeaEnd;
    private String eveningTeaStart;
    private String eveningTeaEnd;
    private boolean lockDuringBreak;
    private boolean showBreakTimer;
    private boolean autoResume;

    // Getters and Setters
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    
    public String getShiftStart() { return shiftStart; }
    public void setShiftStart(String shiftStart) { this.shiftStart = shiftStart; }
    
    public String getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(String shiftEnd) { this.shiftEnd = shiftEnd; }
    
    public int getGraceTime() { return graceTime; }
    public void setGraceTime(int graceTime) { this.graceTime = graceTime; }
    
    public int getWorkingHours() { return workingHours; }
    public void setWorkingHours(int workingHours) { this.workingHours = workingHours; }
    
    public boolean isEnableLunch() { return enableLunch; }
    public void setEnableLunch(boolean enableLunch) { this.enableLunch = enableLunch; }
    
    public String getLunchStart() { return lunchStart; }
    public void setLunchStart(String lunchStart) { this.lunchStart = lunchStart; }
    
    public String getLunchEnd() { return lunchEnd; }
    public void setLunchEnd(String lunchEnd) { this.lunchEnd = lunchEnd; }
    
    public boolean isEnableTeaBreak() { return enableTeaBreak; }
    public void setEnableTeaBreak(boolean enableTeaBreak) { this.enableTeaBreak = enableTeaBreak; }
    
    public String getMorningTeaStart() { return morningTeaStart; }
    public void setMorningTeaStart(String morningTeaStart) { this.morningTeaStart = morningTeaStart; }
    
    public String getMorningTeaEnd() { return morningTeaEnd; }
    public void setMorningTeaEnd(String morningTeaEnd) { this.morningTeaEnd = morningTeaEnd; }
    
    public String getEveningTeaStart() { return eveningTeaStart; }
    public void setEveningTeaStart(String eveningTeaStart) { this.eveningTeaStart = eveningTeaStart; }
    
    public String getEveningTeaEnd() { return eveningTeaEnd; }
    public void setEveningTeaEnd(String eveningTeaEnd) { this.eveningTeaEnd = eveningTeaEnd; }
    
    public boolean isLockDuringBreak() { return lockDuringBreak; }
    public void setLockDuringBreak(boolean lockDuringBreak) { this.lockDuringBreak = lockDuringBreak; }
    
    public boolean isShowBreakTimer() { return showBreakTimer; }
    public void setShowBreakTimer(boolean showBreakTimer) { this.showBreakTimer = showBreakTimer; }
    
    public boolean isAutoResume() { return autoResume; }
    public void setAutoResume(boolean autoResume) { this.autoResume = autoResume; }
}
