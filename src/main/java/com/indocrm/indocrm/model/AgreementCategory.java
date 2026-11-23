// FILE: src/main/java/com/indocrm/indocrm/model/AgreementCategory.java
// NEW FILE — the 5 agreement categories shown in Super Admin → Agreement Templates.
package com.indocrm.indocrm.model;

import java.util.List;

public enum AgreementCategory {

    ADMIN_DASHBOARD("Admin Dashboard", "DIGITAL_MARKETING",
            "Admission management dashboard agreement (per-admission fee model)",
            "agreement-templates/ADMIN_DASHBOARD.html", FieldSet.SERVICE),

    DIGITAL_MARKETING("Digital Marketing", "DIGITAL_MARKETING",
            "Digital marketing services + admission dashboard agreement",
            "agreement-templates/DIGITAL_MARKETING.html", FieldSet.SERVICE),

    ERP_PRO("ERP – Pro", "ERP",
            "School ERP / web dashboard agreement — Pro plan",
            "agreement-templates/ERP_PRO.html", FieldSet.ERP),

    ERP_PRO_PLUS("ERP – Pro Plus", "ERP",
            "School ERP / web dashboard agreement — Pro Plus plan",
            "agreement-templates/ERP_PRO_PLUS.html", FieldSet.ERP),

    ERP_PRO_MAX("ERP – Pro Max", "ERP",
            "School ERP / web dashboard agreement — Pro Max plan",
            "agreement-templates/ERP_PRO_MAX.html", FieldSet.ERP);

    /** Merge fields AgreementService can fill for each agreement type. */
    public enum FieldSet {
        SERVICE(List.of("agreement_date", "signature_date", "client_name", "client_address", "pincode",
                "authorized_person_name", "authorized_person_designation",
                "plan_name", "yearly_fee", "discount_amount", "net_payable", "gst_percent")),
        ERP(List.of("agreement_date", "signature_date", "client_name", "client_address", "pincode",
                "authorized_person_name", "authorized_person_designation",
                "plan_name", "student_strength", "subscription_fee", "gst_percent", "gst_amount", "total_payable"));

        public final List<String> fields;
        FieldSet(List<String> f) { fields = f; }
    }

    public final String label;
    public final String agreementType;   // "ERP" or "DIGITAL_MARKETING" (drives plans/pricing in AgreementService)
    public final String description;
    public final String resource;        // classpath default content
    public final FieldSet fieldSet;

    AgreementCategory(String label, String agreementType, String description, String resource, FieldSet fieldSet) {
        this.label = label;
        this.agreementType = agreementType;
        this.description = description;
        this.resource = resource;
        this.fieldSet = fieldSet;
    }

    public static AgreementCategory of(String key) {
        if (key == null) throw new IllegalArgumentException("Category is required");
        try {
            return valueOf(key.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown agreement category: " + key);
        }
    }
}
