// FILE: src/main/java/com/indocrm/indocrm/model/Telecaller.java

package com.indocrm.indocrm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indocrm.indocrm.security.RoleUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Document(collection = "telecallers")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Telecaller implements UserDetails {

    @Id
    private String id;

    @Indexed(unique = true)
    private String employeeId;

    @Indexed(unique = true)
    private String username;

    // WRITE_ONLY: the BCrypt hash is accepted from the admin UI (create / reset) but is
    // NEVER sent back to the browser. Previously GET /api/telecallers leaked every hash.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String fullName;
    private String mobile;
    private String email;
    private String role;
    private List<String> districts = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private String joiningDate;
    private String status;
    private int target;

    // NEW: Assigned Twilio Phone Number
    private String assignedPhoneNumber;
    private String phoneNumberStatus; // Active, Inactive, Blocked

    // Permissions
    private Map<String, Map<String, Boolean>> permissions = new HashMap<>();

    // Performance metrics
    private Map<String, Object> performance = new HashMap<>();

    // Work timings
    private WorkTiming workTiming;

    private Date createdAt;
    private Date updatedAt;
    private boolean enabled = true;

    public Telecaller() {}

    // Getters and Setters for all fields

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getDistricts() { return districts; }
    public void setDistricts(List<String> districts) { this.districts = districts; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getJoiningDate() { return joiningDate; }
    public void setJoiningDate(String joiningDate) { this.joiningDate = joiningDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }

    public String getAssignedPhoneNumber() { return assignedPhoneNumber; }
    public void setAssignedPhoneNumber(String assignedPhoneNumber) { this.assignedPhoneNumber = assignedPhoneNumber; }

    public String getPhoneNumberStatus() { return phoneNumberStatus; }
    public void setPhoneNumberStatus(String phoneNumberStatus) { this.phoneNumberStatus = phoneNumberStatus; }

    public Map<String, Map<String, Boolean>> getPermissions() { return permissions; }
    public void setPermissions(Map<String, Map<String, Boolean>> permissions) { this.permissions = permissions; }

    public Map<String, Object> getPerformance() { return performance; }
    public void setPerformance(Map<String, Object> performance) { this.performance = performance; }

    public WorkTiming getWorkTiming() { return workTiming; }
    public void setWorkTiming(WorkTiming workTiming) { this.workTiming = workTiming; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    // A telecaller whose status is "Inactive" must not be able to log in
    // ("Disable Login" in the admin UI only flips the status text).
    @Override
    public boolean isEnabled() {
        return enabled && !"Inactive".equalsIgnoreCase(status == null ? "" : status.trim());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return RoleUtil.authorities(role, "TELECALLER");
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}

