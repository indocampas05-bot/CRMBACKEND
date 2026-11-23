package com.indocrm.indocrm.repository;

import com.indocrm.indocrm.model.Employee;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {
    Optional<Employee> findByUsername(String username);
    List<Employee> findByRole(String role);
    List<Employee> findByAssignedDistrict(String district);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
