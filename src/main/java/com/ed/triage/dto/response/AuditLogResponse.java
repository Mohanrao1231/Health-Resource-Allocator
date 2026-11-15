package com.ed.triage.dto.response;

import com.ed.triage.entity.AuditLog;
import com.ed.triage.enums.AuditActionType;
import com.ed.triage.enums.TriageStatus;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        AuditActionType actionType,
        Long triageRecordId,
        Long patientId,
        Long doctorId,
        String details,
        TriageStatus previousStatus,
        TriageStatus newStatus,
        LocalDateTime timestamp,
        String operator
) {
    public static AuditLogResponse fromEntity(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActionType(),
                log.getTriageRecordId(),
                log.getPatientId(),
                log.getDoctorId(),
                log.getDetails(),
                log.getPreviousStatus(),
                log.getNewStatus(),
                log.getTimestamp(),
                log.getOperator()
        );
    }
}
