package com.indocrm.indocrm.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.Quotation;
public interface QuotationRepository extends MongoRepository<Quotation,String>{
 List<Quotation> findByCreatedByOrderByCreatedAtDesc(String createdBy);
 List<Quotation> findAllByOrderByCreatedAtDesc();
 List<Quotation> findByStatusOrderByCreatedAtDesc(String status);
}
