package com.ed.triage.entity;

import com.ed.triage.enums.AuditActionType;
import com.ed.triage.enums.TriageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immutable audit trail entity logging all triage clinical decisions and queue mutations.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Action type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    private AuditActionType actionType;

    @Column(name = "triage_record_id")
    private Long triageRecordId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "details", length = 1024)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private TriageStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 32)
    private TriageStatus newStatus;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "operator", length = 64)
    private String operator;

    public AuditLog() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog(AuditActionType actionType, Long triageRecordId, Long patientId, Long doctorId,
                    String details, TriageStatus previousStatus, TriageStatus newStatus, String operator) {
        this.actionType = actionType;
        this.triageRecordId = triageRecordId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.details = details;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.operator = operator != null ? operator : "SYSTEM_TRIAGE_ENGINE";
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public void setActionType(AuditActionType actionType) {
        this.actionType = actionType;
    }

    public Long getTriageRecordId() {
        return triageRecordId;
    }

    public void setTriageRecordId(Long triageRecordId) {
        this.triageRecordId = triageRecordId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public TriageStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(TriageStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public TriageStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(TriageStatus newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
