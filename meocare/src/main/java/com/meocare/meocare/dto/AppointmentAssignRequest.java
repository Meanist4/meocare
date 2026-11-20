package com.meocare.meocare.dto;

import jakarta.validation.constraints.NotNull;

public record AppointmentAssignRequest(
        @NotNull(message = "veterinarianId is required")
        Long veterinarianId
) {
}
