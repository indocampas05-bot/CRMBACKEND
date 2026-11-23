package com.indocrm.indocrm.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.AttendanceSession;

public interface AttendanceSessionRepository extends MongoRepository<AttendanceSession, String> {
    List<AttendanceSession> findByAttendanceIdOrderByLoginAtAsc(String attendanceId);
    Optional<AttendanceSession> findFirstByUsernameAndConnectedTrueAndManuallyLoggedOutFalseOrderByLoginAtDesc(String username);
}
