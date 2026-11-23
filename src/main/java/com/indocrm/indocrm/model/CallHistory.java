// FILE: src/main/java/com/indocrm/indocrm/model/CallHistory.java
// NEW FILE

package com.indocrm.indocrm.model;

public class CallHistory {
    private int totalCalls;
    private int connected;
    private int missed;
    private int busy;
    private int switchedOff;
    private int wrongNumber;

    public CallHistory() {
        this.totalCalls = 0;
        this.connected = 0;
        this.missed = 0;
        this.busy = 0;
        this.switchedOff = 0;
        this.wrongNumber = 0;
    }

    public CallHistory(int totalCalls, int connected, int missed, int busy, int switchedOff, int wrongNumber) {
        this.totalCalls = totalCalls;
        this.connected = connected;
        this.missed = missed;
        this.busy = busy;
        this.switchedOff = switchedOff;
        this.wrongNumber = wrongNumber;
    }

    // Getters and Setters
    public int getTotalCalls() { return totalCalls; }
    public void setTotalCalls(int totalCalls) { this.totalCalls = totalCalls; }
    
    public int getConnected() { return connected; }
    public void setConnected(int connected) { this.connected = connected; }
    
    public int getMissed() { return missed; }
    public void setMissed(int missed) { this.missed = missed; }
    
    public int getBusy() { return busy; }
    public void setBusy(int busy) { this.busy = busy; }
    
    public int getSwitchedOff() { return switchedOff; }
    public void setSwitchedOff(int switchedOff) { this.switchedOff = switchedOff; }
    
    public int getWrongNumber() { return wrongNumber; }
    public void setWrongNumber(int wrongNumber) { this.wrongNumber = wrongNumber; }

    @Override
    public String toString() {
        return "CallHistory{" +
                "totalCalls=" + totalCalls +
                ", connected=" + connected +
                ", missed=" + missed +
                ", busy=" + busy +
                ", switchedOff=" + switchedOff +
                ", wrongNumber=" + wrongNumber +
                '}';
    }
}
