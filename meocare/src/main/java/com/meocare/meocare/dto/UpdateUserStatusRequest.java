package com.meocare.meocare.dto;

import com.meocare.meocare.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Admin user status update request for PATCH /api/users/{id}/status.
 */
public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
