package com.indocrm.indocrm.dto;

import java.util.List;

import com.indocrm.indocrm.model.QuotationTemplate.DesignBlock;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateDesign;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateField;

public class QuotationTemplateDesignRequest {
    private String name;
    private TemplateDesign design;
    private List<TemplateField> fields;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TemplateDesign getDesign() { return design; }
    public void setDesign(TemplateDesign design) { this.design = design; }
    public List<TemplateField> getFields() { return fields; }
    public void setFields(List<TemplateField> fields) { this.fields = fields; }
}
