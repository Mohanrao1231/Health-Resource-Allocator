package com.ed.triage.dto.response;

import com.ed.triage.entity.Doctor;
import com.ed.triage.enums.Specialty;
import java.time.LocalDateTime;

public record DoctorResponse(
        Long id,
        String employeeId,
        String name,
        Specialty specialty,
        boolean available,
        String departmentCode,
        String departmentName,
        Long activeTriageRecordId,
        LocalDateTime lastAssignedAt
) {
    public static DoctorResponse fromEntity(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getEmployeeId(),
                doctor.getName(),
                doctor.getSpecialty(),
                doctor.isAvailable(),
                doctor.getDepartment() != null ? doctor.getDepartment().getCode() : null,
                doctor.getDepartment() != null ? doctor.getDepartment().getName() : null,
                doctor.getActiveTriageRecordId(),
                doctor.getLastAssignedAt()
        );
    }
}
