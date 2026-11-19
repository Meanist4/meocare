package com.meocare.meocare.dto;

import com.meocare.meocare.entity.PetImage;
import java.time.LocalDateTime;

public record PetImageResponse(
        Long id,
        Long petId,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static PetImageResponse from(PetImage image) {
        return new PetImageResponse(
                image.getId(),
                image.getPet() != null ? image.getPet().getId() : null,
                image.getImageUrl(),
                image.getCreatedAt()
        );
    }
}
