package com.indocrm.indocrm.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthResponse {
    private String token;
    private String username;
    private String fullName;
    private String role;

    // NEW: districts currently assigned to this telecaller (empty for non-telecaller roles).
    // Sent on login so the telecaller app can show/filter by district immediately,
    // without an extra round trip.
    private List<String> districts = new ArrayList<>();

    public AuthResponse() {}

    public AuthResponse(String token, String username, String fullName, String role) {
        this.token = token;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<String> getDistricts() { return districts; }
    public void setDistricts(List<String> districts) { this.districts = districts != null ? districts : new ArrayList<>(); }
}
