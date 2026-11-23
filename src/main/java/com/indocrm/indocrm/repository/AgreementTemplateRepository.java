// FILE: src/main/java/com/indocrm/indocrm/repository/AgreementTemplateRepository.java
// REPLACES existing file — adds findByAgreementTypeAndActiveTrueOrderByCreatedAtDesc
package com.indocrm.indocrm.repository;
import java.util.*; import org.springframework.data.mongodb.repository.MongoRepository; import com.indocrm.indocrm.model.AgreementTemplate;
public interface AgreementTemplateRepository extends MongoRepository<AgreementTemplate,String>{
 List<AgreementTemplate> findByActiveTrueOrderByCreatedAtDesc();
 List<AgreementTemplate> findByAgreementTypeAndActiveTrueOrderByCreatedAtDesc(String agreementType);
 boolean existsByNameIgnoreCase(String name);
 Optional<AgreementTemplate> findByNameIgnoreCase(String name);
 Optional<AgreementTemplate> findFirstByCategoryAndActiveTrue(String category);
}
