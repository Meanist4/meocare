package com.meocare.meocare.dto;

import com.meocare.meocare.entity.HealthUpdate;
import java.time.LocalDateTime;
import java.util.List;

public record HealthUpdateResponse(
        Long id,
        Long medicalRecordId,
        Long petId,
        String petName,
        Long veterinarianId,
        String veterinarianName,
        String healthStatus,
        String description,
        List<HealthUpdateImageResponse> images,
        LocalDateTime createdAt
) {
    public static HealthUpdateResponse from(HealthUpdate h) {
        return from(h, List.of());
    }

    public static HealthUpdateResponse from(HealthUpdate h, List<HealthUpdateImageResponse> images) {
        Long petId = (h.getMedicalRecord() != null && h.getMedicalRecord().getPet() != null)
                ? h.getMedicalRecord().getPet().getId()
                : null;
        String petName = (h.getMedicalRecord() != null && h.getMedicalRecord().getPet() != null)
                ? h.getMedicalRecord().getPet().getName()
                : null;
        return new HealthUpdateResponse(
                h.getId(),
                h.getMedicalRecord() != null ? h.getMedicalRecord().getId() : null,
                petId,
                petName,
                h.getVeterinarian() != null ? h.getVeterinarian().getId() : null,
                h.getVeterinarian() != null ? h.getVeterinarian().getFullName() : null,
                h.getHealthStatus(),
                h.getDescription(),
                images != null ? images : List.of(),
                h.getCreatedAt()
        );
    }
}
