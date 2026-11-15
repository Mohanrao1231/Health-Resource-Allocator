package com.ed.triage.dto.request;

import jakarta.validation.constraints.NotNull;

public record EnqueueRequest(
        @NotNull(message = "Triage record ID is required")
        Long triageRecordId,
        String operator
) {
}
