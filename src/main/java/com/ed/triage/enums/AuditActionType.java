package com.ed.triage.enums;

/**
 * Action classifications for the emergency triage audit trail and undo stack.
 */
public enum AuditActionType {
    ASSESSMENT_CREATED,
    ENQUEUED,
    ASSIGNED_TO_DOCTOR,
    STATUS_UPDATED,
    DEQUEUED,
    UNDO_EXECUTED,
    DOCTOR_AVAILABILITY_CHANGED
}
