// FILE: src/main/java/com/indocrm/indocrm/model/AgreementPlan.java
// NEW FILE
package com.indocrm.indocrm.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A selectable plan/price-point for an agreement.
 *
 * agreementType is either "ERP" or "DIGITAL_MARKETING". The Add Agreement
 * form loads plans filtered by the agreement type the user picked, and the
 * selected plan's price is what gets merged into the PDF — pricing is never
 * hard-coded in PDF generation code.
 *
 * For ERP plans, minStudentStrength/maxStudentStrength let the UI suggest a
 * tier automatically once the telecaller enters the school's student count
 * (nullable = "no bound" e.g. maxStudentStrength null = unlimited).
 *
 * For Digital Marketing plans, discountAmount is optional (0 = no discount).
 */
@Document(collection = "agreement_plans")
public class AgreementPlan {

    @Id
    private String id;

    @Indexed
    private String agreementType; // "ERP" | "DIGITAL_MARKETING"

    private String planName;
    private String description;

    private double feeAmount;         // yearly professional fee (DM) / subscription fee (ERP)
    private double discountAmount;    // DM only; 0 if not applicable
    private double gstPercent = 18.0; // applies to both

    private Integer minStudentStrength; // ERP only, inclusive, null = no lower bound
    private Integer maxStudentStrength; // ERP only, inclusive, null = no upper bound

    private int displayOrder;
    private boolean active = true;
    private boolean systemPlan; // seeded default, shown as reference — still editable by admin

    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String v) { id = v; }
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
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public boolean isSystemPlan() { return systemPlan; }
    public void setSystemPlan(boolean v) { systemPlan = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { updatedAt = v; }
}
