package com.ed.triage.enums;

/**
 * Lifecycle status of a patient triage workflow in the Emergency Department.
 */
public enum TriageStatus {
    ASSESSED("Initial triage assessment completed, awaiting queue placement"),
    WAITING_IN_QUEUE("Actively waiting in priority queue for physician assignment"),
    IN_CONSULTATION("Assigned to physician and currently undergoing emergency care"),
    COMPLETED("Emergency consultation, treatment, or admission completed"),
    CANCELLED("Triage request cancelled or patient left without being seen (LWBS)"),
    TRANSFERRED("Patient transferred to specialized ICU, OR, or external medical center");

    private final String description;

    TriageStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
