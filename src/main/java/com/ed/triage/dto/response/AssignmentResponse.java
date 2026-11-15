package com.ed.triage.dto.response;

import com.ed.triage.entity.Doctor;
import com.ed.triage.entity.TriageRecord;
import com.ed.triage.enums.EsiLevel;
import com.ed.triage.enums.Specialty;
import java.time.LocalDateTime;

public record AssignmentResponse(
        Long triageRecordId,
        Long patientId,
        String patientMrn,
        String patientName,
        Long doctorId,
        String doctorEmployeeId,
        String doctorName,
        Specialty doctorSpecialty,
        EsiLevel esiLevel,
        Integer priorityScore,
        LocalDateTime consultationStartedAt,
        String message
) {
    public static AssignmentResponse from(TriageRecord record, Doctor doctor, String message) {
        return new AssignmentResponse(
                record.getId(),
                record.getPatient() != null ? record.getPatient().getId() : null,
                record.getPatient() != null ? record.getPatient().getMrn() : null,
                record.getPatient() != null ? record.getPatient().getFullName() : null,
                doctor.getId(),
                doctor.getEmployeeId(),
                doctor.getName(),
                doctor.getSpecialty(),
                record.getEsiLevel(),
                record.getPriorityScore(),
                record.getConsultationStartedAt(),
                message
        );
    }
}
