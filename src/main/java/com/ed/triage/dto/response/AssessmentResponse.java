package com.ed.triage.dto.response;

import com.ed.triage.entity.TriageRecord;
import com.ed.triage.entity.Vitals;
import com.ed.triage.enums.EsiLevel;
import com.ed.triage.enums.TriageStatus;
import java.time.LocalDateTime;

public record AssessmentResponse(
        Long triageRecordId,
        Long patientId,
        String patientMrn,
        String patientName,
        int patientAge,
        EsiLevel esiLevel,
        String esiTitle,
        String clinicalDescription,
        Integer priorityScore,
        TriageStatus status,
        int targetWaitTimeMinutes,
        Integer estimatedResources,
        LocalDateTime arrivalTime,
        LocalDateTime assessedAt,
        Vitals vitals,
        String chiefComplaint,
        String clinicalNotes
) {
    public static AssessmentResponse fromEntity(TriageRecord record) {
        return new AssessmentResponse(
                record.getId(),
                record.getPatient() != null ? record.getPatient().getId() : null,
                record.getPatient() != null ? record.getPatient().getMrn() : null,
                record.getPatient() != null ? record.getPatient().getFullName() : null,
                record.getPatient() != null ? record.getPatient().getAge() : 0,
                record.getEsiLevel(),
                record.getEsiLevel().getTitle(),
                record.getEsiLevel().getClinicalDescription(),
                record.getPriorityScore(),
                record.getStatus(),
                record.getEsiLevel().getTargetWaitTimeMinutes(),
                record.getEstimatedResources(),
                record.getArrivalTime(),
                record.getAssessedAt(),
                record.getVitals(),
                record.getChiefComplaint(),
                record.getClinicalNotes()
        );
    }
}
