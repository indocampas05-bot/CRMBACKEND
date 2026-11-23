// FILE: src/main/java/com/indocrm/indocrm/config/ExotelProperties.java
// Reads Exotel credentials from application.properties (DON'T hardcode them in code / DON'T commit to git)

package com.indocrm.indocrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "exotel")
public class ExotelProperties {

    // Your Exotel Account SID (from Exotel dashboard -> Settings -> API Settings)
    private String sid;

    // API Key acts as username for Basic Auth
    private String apiKey;

    // API Token acts as password for Basic Auth
    private String apiToken;

    // Your purchased Exophone / virtual number shown as Caller ID to the customer
    // Format Exotel expects, e.g. "01141234567" or "+91XXXXXXXXXX" - check your Exotel dashboard
    private String exophone;

    // Exotel API base url - most accounts use api.exotel.com, some regions use api.in.exotel.com
    // Check "API Settings" page on your Exotel dashboard for the exact subdomain assigned to you
    private String apiBaseUrl = "https://api.exotel.com";

    // Publicly reachable base URL of THIS backend, used for Exotel status callbacks
    // e.g. https://yourdomain.com  (must be public, not 192.168.x.x - use ngrok while testing locally)
    private String callbackBaseUrl;

    // Whether to record calls (Exotel charges extra per recording minute - keep false if not needed)
    private boolean recordCalls = false;

    public String getSid() { return sid; }
    public void setSid(String sid) { this.sid = sid; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

    public String getExophone() { return exophone; }
    public void setExophone(String exophone) { this.exophone = exophone; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getCallbackBaseUrl() { return callbackBaseUrl; }
    public void setCallbackBaseUrl(String callbackBaseUrl) { this.callbackBaseUrl = callbackBaseUrl; }

    public boolean isRecordCalls() { return recordCalls; }
    public void setRecordCalls(boolean recordCalls) { this.recordCalls = recordCalls; }
}
