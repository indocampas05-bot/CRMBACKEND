// FILE: src/main/java/com/indocrm/indocrm/dto/DistrictAssignmentDTO.java

package com.indocrm.indocrm.dto;

public class DistrictAssignmentDTO {
    private String districtName;
    private String employeeUsername;

    public DistrictAssignmentDTO() {}

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
    public String getEmployeeUsername() { return employeeUsername; }
    public void setEmployeeUsername(String employeeUsername) { this.employeeUsername = employeeUsername; }
}
