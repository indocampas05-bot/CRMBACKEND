package com.indocrm.indocrm.dto;

import java.util.List;
import com.indocrm.indocrm.model.QuotationTemplate.TemplateField;

public class QuotationTemplateFieldsRequest {
    private List<TemplateField> fields;
    public List<TemplateField> getFields(){return fields;} public void setFields(List<TemplateField> v){fields=v;}
}
