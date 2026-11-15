package com.ed.triage.dto.request;

import com.ed.triage.entity.Vitals;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Incoming request payload to perform initial emergency triage assessment.
 */
public record AssessmentRequest(
        Long patientId,
        String patientMrn,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        @NotNull(message = "Patient vital signs are mandatory for triage scoring")
        @Valid
        Vitals vitals,
        @NotBlank(message = "Chief complaint cannot be empty")
        String chiefComplaint,
        Integer estimatedResources,
        String clinicalNotes,
        LocalDateTime arrivalTime,
        boolean autoEnqueue
) {
}
