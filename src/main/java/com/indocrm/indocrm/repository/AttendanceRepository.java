package com.indocrm.indocrm.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import com.indocrm.indocrm.model.AttendanceRecord;

@Repository
public interface AttendanceRepository extends MongoRepository<AttendanceRecord, String> {
    Optional<AttendanceRecord> findByEmployeeIdAndDate(String employeeId, String date);
    List<AttendanceRecord> findByEmployeeIdOrderByDateDesc(String employeeId);
    List<AttendanceRecord> findAllByDateOrderByFirstLoginAtAsc(String date);
    List<AttendanceRecord> findAllByOrderByDateDesc();
    Optional<AttendanceRecord> findByUsernameAndDate(String username, String date);
}
