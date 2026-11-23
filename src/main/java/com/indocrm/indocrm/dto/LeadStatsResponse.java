// FILE: src/main/java/com/indocrm/indocrm/dto/LeadStatsResponse.java
// REPLACES existing with additional stats

package com.indocrm.indocrm.dto;

public class LeadStatsResponse {
    private long total;
    private long newLeads;
    private long inProgress;
    private long interested;
    private long notInterested;
    private long notExisting;
    private long converted;
    private long demoScheduled;
    private long callBack;

    public LeadStatsResponse() {}

    // Getters and Setters
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getNewLeads() { return newLeads; }
    public void setNewLeads(long newLeads) { this.newLeads = newLeads; }
    public long getInProgress() { return inProgress; }
    public void setInProgress(long inProgress) { this.inProgress = inProgress; }
    public long getInterested() { return interested; }
    public void setInterested(long interested) { this.interested = interested; }
    public long getNotInterested() { return notInterested; }
    public void setNotInterested(long notInterested) { this.notInterested = notInterested; }
    public long getNotExisting() { return notExisting; }
    public void setNotExisting(long notExisting) { this.notExisting = notExisting; }
    public long getConverted() { return converted; }
    public void setConverted(long converted) { this.converted = converted; }
    public long getDemoScheduled() { return demoScheduled; }
    public void setDemoScheduled(long demoScheduled) { this.demoScheduled = demoScheduled; }
    public long getCallBack() { return callBack; }
    public void setCallBack(long callBack) { this.callBack = callBack; }
}
