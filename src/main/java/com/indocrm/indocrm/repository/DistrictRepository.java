package com.indocrm.indocrm.repository;

import com.indocrm.indocrm.model.District;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends MongoRepository<District, String> {
    Optional<District> findByName(String name);
    List<District> findByAssignedTo(String assignedTo);
    List<District> findByAssignedToIsNull();
    boolean existsByName(String name);
}
