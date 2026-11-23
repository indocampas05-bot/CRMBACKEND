// FILE: src/main/java/com/indocrm/indocrm/dto/EmployeeDTO.java

package com.indocrm.indocrm.dto;

public class EmployeeDTO {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String assignedDistrict;
    private boolean enabled;

    public EmployeeDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAssignedDistrict() { return assignedDistrict; }
    public void setAssignedDistrict(String assignedDistrict) { this.assignedDistrict = assignedDistrict; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
