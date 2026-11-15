package com.ed.triage.service;

import com.ed.triage.dto.response.AuditLogResponse;
import com.ed.triage.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLogResponse> getRecentAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc().stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsForTriageRecord(Long triageRecordId) {
        return auditLogRepository.findByTriageRecordIdOrderByTimestampDesc(triageRecordId).stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }

    public List<AuditLogResponse> getAuditLogsForPatient(Long patientId) {
        return auditLogRepository.findByPatientIdOrderByTimestampDesc(patientId).stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }
}
