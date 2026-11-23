// FILE: src/main/java/com/indocrm/indocrm/repository/AgreementPlanRepository.java
// NEW FILE
package com.indocrm.indocrm.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.AgreementPlan;

public interface AgreementPlanRepository extends MongoRepository<AgreementPlan, String> {
    List<AgreementPlan> findByAgreementTypeAndActiveTrueOrderByDisplayOrderAsc(String agreementType);
    List<AgreementPlan> findAllByOrderByAgreementTypeAscDisplayOrderAsc();
    long countByAgreementType(String agreementType);
}
