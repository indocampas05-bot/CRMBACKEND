package com.indocrm.indocrm.dto;

public class CampaignStatsResponse {
    private String campaign;
    private long count;

    public CampaignStatsResponse() {}

    public CampaignStatsResponse(String campaign, long count) {
        this.campaign = campaign;
        this.count = count;
    }

    public String getCampaign() { return campaign; }
    public void setCampaign(String campaign) { this.campaign = campaign; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
