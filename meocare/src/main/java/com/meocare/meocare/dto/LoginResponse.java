package com.meocare.meocare.dto;

/**
 * Authentication response containing the signed JWT access token and user profile.
 */
public record LoginResponse(
        String message,
        String token,
        UserResponse user
) {
}
