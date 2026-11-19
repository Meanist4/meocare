package com.meocare.meocare.dto;

/**
 * Kết quả đăng nhập. Hiện chưa tích hợp JWT nên token luôn là null;
 * sau này chỉ cần gán access token vào đây mà không đổi cấu trúc response.
 */
public record LoginResponse(
        String message,
        String token,
        UserResponse user
) {
}
