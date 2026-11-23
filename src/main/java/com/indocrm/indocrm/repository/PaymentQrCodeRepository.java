package com.indocrm.indocrm.repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.PaymentQrCode;
public interface PaymentQrCodeRepository extends MongoRepository<PaymentQrCode,String> {}

