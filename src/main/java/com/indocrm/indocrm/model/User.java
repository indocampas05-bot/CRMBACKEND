package com.indocrm.indocrm.model;

import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    private String fullName;

    private String email;

    private String role;

    private boolean enabled = true;

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /*
     * IMPORTANT:
     *
     * Converts:
     *
     * SUPER_ADMIN
     * Super Admin
     * super admin
     * ROLE_SUPER_ADMIN
     *
     * into:
     *
     * ROLE_SUPER_ADMIN
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        String normalizedRole = normalizeRole(role);

        return List.of(
            new SimpleGrantedAuthority(
                "ROLE_" + normalizedRole
            )
        );
    }

    private String normalizeRole(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "USER";
        }

        String normalized = value
            .trim()
            .toUpperCase()
            .replace("-", "_")
            .replace(" ", "_");

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

   
}