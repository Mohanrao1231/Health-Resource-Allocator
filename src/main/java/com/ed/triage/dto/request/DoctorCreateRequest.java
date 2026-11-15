package com.ed.triage.dto.request;

import com.ed.triage.enums.Specialty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DoctorCreateRequest(
        @NotBlank(message = "Employee ID is required")
        String employeeId,
        @NotBlank(message = "Doctor name is required")
        String name,
        @NotNull(message = "Specialty is required")
        Specialty specialty,
        String departmentCode
) {
}
