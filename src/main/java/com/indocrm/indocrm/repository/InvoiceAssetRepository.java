package com.indocrm.indocrm.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.InvoiceAsset;

public interface InvoiceAssetRepository extends MongoRepository<InvoiceAsset, String> {
    Optional<InvoiceAsset> findFirstByTypeOrderByUploadedAtDesc(String type);
    void deleteByType(String type);
}