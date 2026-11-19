package com.meocare.meocare.dto;

import com.meocare.meocare.entity.MedicalRecordImage;
import java.time.LocalDateTime;

public record MedicalRecordImageResponse(
        Long id,
        Long medicalRecordId,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static MedicalRecordImageResponse from(MedicalRecordImage image) {
        return new MedicalRecordImageResponse(
                image.getId(),
                image.getMedicalRecord() != null ? image.getMedicalRecord().getId() : null,
                image.getImageUrl(),
                image.getCreatedAt()
        );
    }
}
