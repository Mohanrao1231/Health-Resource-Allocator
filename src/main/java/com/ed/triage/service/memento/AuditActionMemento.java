package com.ed.triage.service.memento;

import com.ed.triage.entity.TriageRecord;
import com.ed.triage.enums.AuditActionType;
import com.ed.triage.enums.TriageStatus;
import java.time.LocalDateTime;

/**
 * Memento pattern snapshot stored on the LIFO Undo Stack.
 * Holds exact state transitions to allow reversible emergency triage operations.
 */
public record AuditActionMemento(
        AuditActionType actionType,
        Long triageRecordId,
        Long patientId,
        Long previousDoctorId,
        Long newDoctorId,
        TriageStatus previousStatus,
        TriageStatus newStatus,
        boolean wasInQueue,
        boolean shouldRequeueOnUndo,
        TriageRecord snapshotRecord,
        LocalDateTime timestamp,
        String operator
) {
    public static AuditActionMemento forEnqueue(TriageRecord record, String operator) {
        return new AuditActionMemento(
                AuditActionType.ENQUEUED,
                record.getId(),
                record.getPatient() != null ? record.getPatient().getId() : null,
                null,
                null,
                TriageStatus.ASSESSED,
                TriageStatus.WAITING_IN_QUEUE,
                false,
                false, // on undo of enqueue: remove from queue and set back to ASSESSED
                record,
                LocalDateTime.now(),
                operator
        );
    }

    public static AuditActionMemento forDoctorAssignment(TriageRecord record, Long doctorId, String operator) {
        return new AuditActionMemento(
                AuditActionType.ASSIGNED_TO_DOCTOR,
                record.getId(),
                record.getPatient() != null ? record.getPatient().getId() : null,
                null,
                doctorId,
                TriageStatus.WAITING_IN_QUEUE,
                TriageStatus.IN_CONSULTATION,
                true,
                true, // on undo of doctor assignment: remove doctor, set back to WAITING_IN_QUEUE, push back into PriorityQueue
                record,
                LocalDateTime.now(),
                operator
        );
    }

    public static AuditActionMemento forStatusUpdate(TriageRecord record, TriageStatus prevStatus, TriageStatus newStatus, String operator) {
        boolean wasInQ = prevStatus == TriageStatus.WAITING_IN_QUEUE;
        boolean shouldReQ = wasInQ && newStatus != TriageStatus.WAITING_IN_QUEUE;
        return new AuditActionMemento(
                AuditActionType.STATUS_UPDATED,
                record.getId(),
                record.getPatient() != null ? record.getPatient().getId() : null,
                record.getAssignedDoctor() != null ? record.getAssignedDoctor().getId() : null,
                null,
                prevStatus,
                newStatus,
                wasInQ,
                shouldReQ,
                record,
                LocalDateTime.now(),
                operator
        );
    }
}
