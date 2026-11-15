package com.ed.triage.dto.request;

import com.ed.triage.enums.TriageStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "New triage status is required")
        TriageStatus status,
        String clinicalNotes,
        String operator
) {
}
