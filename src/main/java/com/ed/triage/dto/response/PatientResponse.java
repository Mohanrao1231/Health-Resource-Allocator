package com.ed.triage.dto.response;

import com.ed.triage.entity.Patient;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long id,
        String mrn,
        String firstName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        int age,
        String gender,
        String contactNumber,
        String allergies,
        String medicalHistory,
        LocalDateTime registeredAt
) {
    public static PatientResponse fromEntity(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getMrn(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getAge(),
                patient.getGender(),
                patient.getContactNumber(),
                patient.getAllergies(),
                patient.getMedicalHistory(),
                patient.getRegisteredAt()
        );
    }
}
