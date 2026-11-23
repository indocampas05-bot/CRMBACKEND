package com.indocrm.indocrm.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.Demo;

public interface DemoRepository extends MongoRepository<Demo, String> {
    List<Demo> findByCreatedByOrderByCreatedAtDesc(String createdBy);
}

