package com.indocrm.indocrm.repository;

import com.indocrm.indocrm.model.UploadHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadHistoryRepository extends MongoRepository<UploadHistory, String> {
    List<UploadHistory> findByUploadedByOrderByUploadedAtDesc(String uploadedBy);
    List<UploadHistory> findAllByOrderByUploadedAtDesc();
}
