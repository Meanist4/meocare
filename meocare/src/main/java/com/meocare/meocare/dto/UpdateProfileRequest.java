package com.meocare.meocare.dto;

import jakarta.validation.constraints.Size;

/**
 * Self-service profile update payload for PUT /api/auth/me.
 * Role, email, and status cannot be modified via this request.
 */
public record UpdateProfileRequest(
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phone,

        @Size(max = 500, message = "Avatar URL must be at most 500 characters")
        String avatarUrl,

        String currentPassword,

        @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
        String newPassword
) {
}
