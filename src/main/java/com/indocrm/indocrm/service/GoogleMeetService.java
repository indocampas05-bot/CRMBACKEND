package com.indocrm.indocrm.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indocrm.indocrm.model.GoogleOAuthToken;
import com.indocrm.indocrm.repository.GoogleOAuthTokenRepository;

@Service
public class GoogleMeetService {
    private static final String SCOPE = "https://www.googleapis.com/auth/meetings.space.created";
    private final GoogleOAuthTokenRepository tokenRepo;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${google.meet.client-id:}") private String clientId;
    @Value("${google.meet.client-secret:}") private String clientSecret;
    @Value("${google.meet.redirect-uri:http://localhost:5000/api/google-meet/oauth/callback}") private String redirectUri;

    public GoogleMeetService(GoogleOAuthTokenRepository tokenRepo, ObjectMapper mapper) {
        this.tokenRepo = tokenRepo; this.mapper = mapper;
    }

    public String authorizationUrl(String username) {
        if (clientId.isBlank() || clientSecret.isBlank()) throw new IllegalStateException("Google Meet OAuth credentials are not configured on the backend");
        GoogleOAuthToken token = tokenRepo.findByUsername(username).orElseGet(GoogleOAuthToken::new);
        token.setUsername(username); token.setState(UUID.randomUUID().toString()); tokenRepo.save(token);
        return "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + enc(clientId)
            + "&redirect_uri=" + enc(redirectUri) + "&response_type=code&access_type=offline&prompt=consent"
            + "&scope=" + enc(SCOPE) + "&state=" + enc(token.getState());
    }

    public void handleCallback(String code, String state) throws Exception {
        GoogleOAuthToken token = tokenRepo.findByState(state).orElseThrow(() -> new IllegalArgumentException("Invalid or expired Google OAuth state"));
        String body = "code="+enc(code)+"&client_id="+enc(clientId)+"&client_secret="+enc(clientSecret)+"&redirect_uri="+enc(redirectUri)+"&grant_type=authorization_code";
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        JsonNode json = mapper.readTree(http.send(req,HttpResponse.BodyHandlers.ofString()).body());
        if (json.has("error")) throw new IllegalStateException("Google OAuth failed: "+json.path("error_description").asText(json.path("error").asText()));
        token.setAccessToken(json.path("access_token").asText());
        String refresh = json.path("refresh_token").asText(null); if (refresh != null) token.setRefreshToken(refresh);
        token.setExpiresAtEpochMs(Instant.now().toEpochMilli()+json.path("expires_in").asLong(3600)*1000L);
        token.setState(null); tokenRepo.save(token);
    }

    public boolean connected(String username) { return tokenRepo.findByUsername(username).map(t -> t.getRefreshToken()!=null || t.accessTokenValid()).orElse(false); }

    public String createMeeting(String username) throws Exception {
        GoogleOAuthToken token = tokenRepo.findByUsername(username).orElseThrow(() -> new GoogleNotConnectedException(authorizationUrl(username)));
        String access = validAccessToken(token);
        String jsonBody = "{}";
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://meet.googleapis.com/v2/spaces"))
            .header("Authorization","Bearer "+access).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> res = http.send(req,HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(res.body());
        if (res.statusCode() >= 300) throw new IllegalStateException("Google Meet API failed: "+json.path("error").path("message").asText(res.body()));
        String uri = json.path("meetingUri").asText(); if (uri.isBlank()) throw new IllegalStateException("Google Meet did not return meetingUri");
        return uri;
    }

 private String validAccessToken(GoogleOAuthToken token) throws Exception {
    if (token.accessTokenValid()) return token.getAccessToken();

    if (token.getRefreshToken() == null || token.getRefreshToken().isBlank())
        throw new GoogleNotConnectedException(authorizationUrl(token.getUsername()));

    if (clientId.isBlank() || clientSecret.isBlank())
        throw new IllegalStateException("Google Meet OAuth credentials are not configured on the backend");

    String body = "client_id=" + enc(clientId)
        + "&client_secret=" + enc(clientSecret)
        + "&refresh_token=" + enc(token.getRefreshToken())
        + "&grant_type=refresh_token";

    HttpRequest req = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build();

    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
    JsonNode json = mapper.readTree(res.body());

    if (res.statusCode() >= 300 || json.has("error")) {
        // 400 = invalid_grant / invalid_request -> stored refresh token is dead.
        // Wipe it and make the user re-authorize (frontend opens the Google popup on 409).
        if (res.statusCode() == 400) {
            token.setAccessToken(null);
            token.setRefreshToken(null);
            token.setExpiresAtEpochMs(0);
            tokenRepo.save(token);
            throw new GoogleNotConnectedException(authorizationUrl(token.getUsername()));
        }
        // 401 invalid_client etc. = real config problem (wrong client id/secret)
        throw new IllegalStateException("Google token refresh failed: "
            + json.path("error_description").asText(json.path("error").asText(res.body())));
    }

    token.setAccessToken(json.path("access_token").asText());
    token.setExpiresAtEpochMs(Instant.now().toEpochMilli() + json.path("expires_in").asLong(3600) * 1000L);
    tokenRepo.save(token);
    return token.getAccessToken();
}
    private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    public static class GoogleNotConnectedException extends RuntimeException { private final String authorizationUrl; public GoogleNotConnectedException(String u){super("Google Meet is not connected");authorizationUrl=u;} public String getAuthorizationUrl(){return authorizationUrl;} }
}

