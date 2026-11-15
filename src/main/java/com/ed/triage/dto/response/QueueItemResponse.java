package com.ed.triage.dto.response;

import com.ed.triage.entity.TriageRecord;
import com.ed.triage.enums.EsiLevel;
import com.ed.triage.enums.TriageStatus;
import java.time.Duration;
import java.time.LocalDateTime;

public record QueueItemResponse(
        int position,
        Long triageRecordId,
        Long patientId,
        String patientMrn,
        String patientName,
        int patientAge,
        String gender,
        EsiLevel esiLevel,
        Integer priorityScore,
        TriageStatus status,
        String chiefComplaint,
        LocalDateTime arrivalTime,
        long waitingMinutes,
        Integer heartRate,
        String bloodPressure,
        Double oxygenSaturation,
        Integer painScore
) {
    public static QueueItemResponse fromEntity(int position, TriageRecord record) {
        long waitMins = record.getArrivalTime() != null ?
                Duration.between(record.getArrivalTime(), LocalDateTime.now()).toMinutes() : 0;

        String bp = (record.getVitals() != null && record.getVitals().getSystolicBp() != null) ?
                record.getVitals().getSystolicBp() + "/" + record.getVitals().getDiastolicBp() : "N/A";

        return new QueueItemResponse(
                position,
                record.getId(),
                record.getPatient() != null ? record.getPatient().getId() : null,
                record.getPatient() != null ? record.getPatient().getMrn() : null,
                record.getPatient() != null ? record.getPatient().getFullName() : null,
                record.getPatient() != null ? record.getPatient().getAge() : 0,
                record.getPatient() != null ? record.getPatient().getGender() : null,
                record.getEsiLevel(),
                record.getPriorityScore(),
                record.getStatus(),
                record.getChiefComplaint(),
                record.getArrivalTime(),
                waitMins,
                record.getVitals() != null ? record.getVitals().getHeartRate() : null,
                bp,
                record.getVitals() != null ? record.getVitals().getOxygenSaturation() : null,
                record.getVitals() != null ? record.getVitals().getPainScore() : null
        );
    }
}
