package com.meocare.meocare.dto;

import com.meocare.meocare.entity.HealthUpdateImage;
import java.time.LocalDateTime;

public record HealthUpdateImageResponse(
        Long id,
        Long healthUpdateId,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static HealthUpdateImageResponse from(HealthUpdateImage image) {
        return new HealthUpdateImageResponse(
                image.getId(),
                image.getHealthUpdate() != null ? image.getHealthUpdate().getId() : null,
                image.getImageUrl(),
                image.getCreatedAt()
        );
    }
}
