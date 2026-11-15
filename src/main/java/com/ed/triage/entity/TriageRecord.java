package com.ed.triage.entity;

import com.ed.triage.enums.EsiLevel;
import com.ed.triage.enums.TriageStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Core clinical triage assessment record representing a patient's emergency encounter.
 */
@Entity
@Table(name = "triage_records")
public class TriageRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Patient reference is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "heartRate", column = @Column(name = "vital_heart_rate")),
            @AttributeOverride(name = "systolicBp", column = @Column(name = "vital_systolic_bp")),
            @AttributeOverride(name = "diastolicBp", column = @Column(name = "vital_diastolic_bp")),
            @AttributeOverride(name = "respiratoryRate", column = @Column(name = "vital_respiratory_rate")),
            @AttributeOverride(name = "oxygenSaturation", column = @Column(name = "vital_oxygen_saturation")),
            @AttributeOverride(name = "temperature", column = @Column(name = "vital_temperature")),
            @AttributeOverride(name = "painScore", column = @Column(name = "vital_pain_score")),
            @AttributeOverride(name = "gcsScore", column = @Column(name = "vital_gcs_score"))
    })
    private Vitals vitals;

    @NotBlank(message = "Chief complaint is required")
    @Column(name = "chief_complaint", nullable = false, length = 512)
    private String chiefComplaint;

    @NotNull(message = "ESI level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "esi_level", nullable = false, length = 16)
    private EsiLevel esiLevel;

    @NotNull(message = "Priority score is required")
    @Column(name = "priority_score", nullable = false)
    private Integer priorityScore; // Higher score = higher priority in queue

    @NotNull(message = "Triage status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TriageStatus status = TriageStatus.ASSESSED;

    @Column(name = "estimated_resources")
    private Integer estimatedResources = 0;

    @Column(name = "clinical_notes", length = 1024)
    private String clinicalNotes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_doctor_id")
    private Doctor assignedDoctor;

    @NotNull(message = "Arrival time is required")
    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;

    @Column(name = "consultation_started_at")
    private LocalDateTime consultationStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public TriageRecord() {
        this.arrivalTime = LocalDateTime.now();
        this.assessedAt = LocalDateTime.now();
        this.status = TriageStatus.ASSESSED;
    }

    public TriageRecord(Patient patient, Vitals vitals, String chiefComplaint, EsiLevel esiLevel,
                        Integer priorityScore, Integer estimatedResources, String clinicalNotes, LocalDateTime arrivalTime) {
        this.patient = patient;
        this.vitals = vitals;
        this.chiefComplaint = chiefComplaint;
        this.esiLevel = esiLevel;
        this.priorityScore = priorityScore;
        this.estimatedResources = estimatedResources != null ? estimatedResources : 0;
        this.clinicalNotes = clinicalNotes;
        this.arrivalTime = arrivalTime != null ? arrivalTime : LocalDateTime.now();
        this.assessedAt = LocalDateTime.now();
        this.status = TriageStatus.ASSESSED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Vitals getVitals() {
        return vitals;
    }

    public void setVitals(Vitals vitals) {
        this.vitals = vitals;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public EsiLevel getEsiLevel() {
        return esiLevel;
    }

    public void setEsiLevel(EsiLevel esiLevel) {
        this.esiLevel = esiLevel;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public TriageStatus getStatus() {
        return status;
    }

    public void setStatus(TriageStatus status) {
        this.status = status;
    }

    public Integer getEstimatedResources() {
        return estimatedResources;
    }

    public void setEstimatedResources(Integer estimatedResources) {
        this.estimatedResources = estimatedResources;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
    }

    public Doctor getAssignedDoctor() {
        return assignedDoctor;
    }

    public void setAssignedDoctor(Doctor assignedDoctor) {
        this.assignedDoctor = assignedDoctor;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime assessedAt) {
        this.assessedAt = assessedAt;
    }

    public LocalDateTime getConsultationStartedAt() {
        return consultationStartedAt;
    }

    public void setConsultationStartedAt(LocalDateTime consultationStartedAt) {
        this.consultationStartedAt = consultationStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriageRecord that = (TriageRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TriageRecord{" +
                "id=" + id +
                ", patient=" + (patient != null ? patient.getMrn() : "null") +
                ", esiLevel=" + esiLevel +
                ", priorityScore=" + priorityScore +
                ", status=" + status +
                ", arrivalTime=" + arrivalTime +
                '}';
    }
}
