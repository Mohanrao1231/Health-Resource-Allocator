package com.ed.triage.entity;

import com.ed.triage.enums.Specialty;
import jakarta.persistence.Column;
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
 * Emergency Department physician available for triage patient consultations.
 */
@Entity
@Table(name = "doctors")
public class Doctor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Employee ID is required")
    @Column(name = "employee_id", unique = true, nullable = false, length = 64)
    private String employeeId;

    @NotBlank(message = "Doctor name is required")
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @NotNull(message = "Specialty is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", nullable = false, length = 64)
    private Specialty specialty;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "active_triage_record_id")
    private Long activeTriageRecordId;

    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;

    public Doctor() {
    }

    public Doctor(String employeeId, String name, Specialty specialty, Department department) {
        this.employeeId = employeeId;
        this.name = name;
        this.specialty = specialty;
        this.department = department;
        this.available = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public void setSpecialty(Specialty specialty) {
        this.specialty = specialty;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Long getActiveTriageRecordId() {
        return activeTriageRecordId;
    }

    public void setActiveTriageRecordId(Long activeTriageRecordId) {
        this.activeTriageRecordId = activeTriageRecordId;
    }

    public LocalDateTime getLastAssignedAt() {
        return lastAssignedAt;
    }

    public void setLastAssignedAt(LocalDateTime lastAssignedAt) {
        this.lastAssignedAt = lastAssignedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Doctor doctor = (Doctor) o;
        return Objects.equals(id, doctor.id) || Objects.equals(employeeId, doctor.employeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId);
    }

    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + id +
                ", employeeId='" + employeeId + '\'' +
                ", name='" + name + '\'' +
                ", specialty=" + specialty +
                ", available=" + available +
                '}';
    }
}
