// src/main/java/com/indocrm/indocrm/dto/AuthRequest.java

package com.indocrm.indocrm.dto;

public class AuthRequest {
    private String username;
    private String password;

    // ADDED: needed so /api/auth/register can accept the fullName/email fields
    // the frontend already sends. Login requests simply leave these null.
    private String fullName;
    private String email;

    public AuthRequest() {}

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
