package com.ed.triage.repository;

import com.ed.triage.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTriageRecordIdOrderByTimestampDesc(Long triageRecordId);
    List<AuditLog> findByPatientIdOrderByTimestampDesc(Long patientId);
    List<AuditLog> findTop50ByOrderByTimestampDesc();
}
