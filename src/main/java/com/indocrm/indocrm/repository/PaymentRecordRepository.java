package com.indocrm.indocrm.repository;
import java.util.*; import org.springframework.data.mongodb.repository.MongoRepository; import com.indocrm.indocrm.model.PaymentRecord;
public interface PaymentRecordRepository extends MongoRepository<PaymentRecord,String>{ Optional<PaymentRecord> findByLeadId(String leadId); }

