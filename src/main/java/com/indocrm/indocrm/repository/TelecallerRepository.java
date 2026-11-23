// FILE: src/main/java/com/indocrm/indocrm/repository/TelecallerRepository.java

package com.indocrm.indocrm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.indocrm.indocrm.model.Telecaller;

@Repository
public interface TelecallerRepository extends MongoRepository<Telecaller, String> {
    Optional<Telecaller> findByUsername(String username);
    Optional<Telecaller> findByEmployeeId(String employeeId);
    List<Telecaller> findByStatus(String status);
    List<Telecaller> findByDistrictsContaining(String district);
    
    // NEW: Phone number methods
    Optional<Telecaller> findByAssignedPhoneNumber(String phoneNumber);
    boolean existsByAssignedPhoneNumber(String phoneNumber);
    List<Telecaller> findByPhoneNumberStatus(String status);
    
    boolean existsByUsername(String username);
    boolean existsByEmployeeId(String employeeId);
}
