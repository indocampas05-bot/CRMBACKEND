package com.indocrm.indocrm.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.AttendanceAuditLog;

public interface AttendanceAuditLogRepository extends MongoRepository<AttendanceAuditLog, String> {
    List<AttendanceAuditLog> findByAttendanceIdOrderByChangedAtDesc(String attendanceId);
}
