package com.meocare.meocare.dto;

import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import java.time.LocalDateTime;

/**
 * Không bao giờ chứa password.
 * Lưu ý: {@link #from(User)} đọc user.getRole() (LAZY), nên User phải được nạp kèm role
 * (ví dụ UserRepository.findWithRoleByEmail) hoặc gọi trong transaction.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        RoleName role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
