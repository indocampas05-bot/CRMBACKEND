// FILE: src/main/java/com/indocrm/indocrm/dto/AgreementPlanRequest.java
// NEW FILE
package com.indocrm.indocrm.dto;

public class AgreementPlanRequest {
    private String agreementType;
    private String planName;
    private String description;
    private double feeAmount;
    private double discountAmount;
    private double gstPercent = 18.0;
    private Integer minStudentStrength;
    private Integer maxStudentStrength;
    private int displayOrder;

    public String getAgreementType() { return agreementType; }
    public void setAgreementType(String v) { agreementType = v; }
    public String getPlanName() { return planName; }
    public void setPlanName(String v) { planName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public double getFeeAmount() { return feeAmount; }
    public void setFeeAmount(double v) { feeAmount = v; }
    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double v) { discountAmount = v; }
    public double getGstPercent() { return gstPercent; }
    public void setGstPercent(double v) { gstPercent = v; }
    public Integer getMinStudentStrength() { return minStudentStrength; }
    public void setMinStudentStrength(Integer v) { minStudentStrength = v; }
    public Integer getMaxStudentStrength() { return maxStudentStrength; }
    public void setMaxStudentStrength(Integer v) { maxStudentStrength = v; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int v) { displayOrder = v; }
}
