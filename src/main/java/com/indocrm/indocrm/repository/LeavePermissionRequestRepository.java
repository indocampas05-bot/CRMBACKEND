package com.indocrm.indocrm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.indocrm.indocrm.model.LeavePermissionRequest;

public interface LeavePermissionRequestRepository
        extends MongoRepository<LeavePermissionRequest, String> {

    List<LeavePermissionRequest>
    findByUsernameOrderBySubmittedAtDesc(String username);

    Optional<LeavePermissionRequest>
    findByIdAndUsername(String id, String username);

    List<LeavePermissionRequest>
    findAllByOrderBySubmittedAtDesc();

    List<LeavePermissionRequest>
    findByStatusOrderBySubmittedAtDesc(String status);
}