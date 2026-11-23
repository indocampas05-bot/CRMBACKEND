// FILE: src/main/java/com/indocrm/indocrm/dto/CallStatsResponse.java

package com.indocrm.indocrm.dto;

import java.util.Map;

public class CallStatsResponse {
    private int totalCalls;
    private int answered;
    private int notAnswered;
    private int interested;
    private int notInterested;
    private int callBack;
    private int talkSecs;
    private Map<String, Integer> statusCounts;
    private Map<String, Integer> outcomeCounts;
    
    // Getters and Setters
    public int getTotalCalls() { return totalCalls; }
    public void setTotalCalls(int totalCalls) { this.totalCalls = totalCalls; }
    
    public int getAnswered() { return answered; }
    public void setAnswered(int answered) { this.answered = answered; }
    
    public int getNotAnswered() { return notAnswered; }
    public void setNotAnswered(int notAnswered) { this.notAnswered = notAnswered; }
    
    public int getInterested() { return interested; }
    public void setInterested(int interested) { this.interested = interested; }
    
    public int getNotInterested() { return notInterested; }
    public void setNotInterested(int notInterested) { this.notInterested = notInterested; }
    
    public int getCallBack() { return callBack; }
    public void setCallBack(int callBack) { this.callBack = callBack; }
    
    public int getTalkSecs() { return talkSecs; }
    public void setTalkSecs(int talkSecs) { this.talkSecs = talkSecs; }
    
    public Map<String, Integer> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Integer> statusCounts) { this.statusCounts = statusCounts; }
    
    public Map<String, Integer> getOutcomeCounts() { return outcomeCounts; }
    public void setOutcomeCounts(Map<String, Integer> outcomeCounts) { this.outcomeCounts = outcomeCounts; }
}
