package com.meocare.meocare.dto;

import com.meocare.meocare.entity.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin staff provisioning request for POST /api/users (VETERINARIAN or ADMIN only).
 */
public record CreateStaffUserRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phone,

        @Size(max = 500, message = "Avatar URL must be at most 500 characters")
        String avatarUrl,

        @NotNull(message = "Role is required")
        RoleName role
) {
}
