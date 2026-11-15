package com.ed.triage.dto.request;

import jakarta.validation.constraints.NotNull;

public record DoctorAvailabilityRequest(
        @NotNull(message = "Availability status is required")
        Boolean available
) {
}
