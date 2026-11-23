package com.indocrm.indocrm.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "google_oauth_tokens")
public class GoogleOAuthToken {
    @Id private String id;
    private String username;
    private String state;
    private String accessToken;
    private String refreshToken;
    private long expiresAtEpochMs;

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getState(){return state;} public void setState(String v){state=v;}
    public String getAccessToken(){return accessToken;} public void setAccessToken(String v){accessToken=v;}
    public String getRefreshToken(){return refreshToken;} public void setRefreshToken(String v){refreshToken=v;}
    public long getExpiresAtEpochMs(){return expiresAtEpochMs;} public void setExpiresAtEpochMs(long v){expiresAtEpochMs=v;}
    public boolean accessTokenValid(){return accessToken != null && !accessToken.isBlank() && expiresAtEpochMs > Instant.now().toEpochMilli()+60000;}
}

