package com.indocrm.indocrm.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.QuotationTemplate;

public interface QuotationTemplateRepository extends MongoRepository<QuotationTemplate, String> {
    List<QuotationTemplate> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}
