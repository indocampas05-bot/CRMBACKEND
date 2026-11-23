package com.indocrm.indocrm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.indocrm.indocrm.model.Invoice;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByPaymentId(String paymentId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findAllByOrderByCreatedAtDesc();
}