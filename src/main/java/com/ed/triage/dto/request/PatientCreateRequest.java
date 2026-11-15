package com.ed.triage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PatientCreateRequest(
        @NotBlank(message = "MRN is required")
        String mrn,
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,
        String gender,
        String contactNumber,
        String allergies,
        String medicalHistory
) {
}
