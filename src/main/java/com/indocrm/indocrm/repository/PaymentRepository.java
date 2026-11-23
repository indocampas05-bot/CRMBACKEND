package com.indocrm.indocrm.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.indocrm.indocrm.model.Payment;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByCreatedBy(String createdBy);
    List<Payment> findByLeadId(String leadId);
}

