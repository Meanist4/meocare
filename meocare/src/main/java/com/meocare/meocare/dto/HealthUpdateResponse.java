package com.meocare.meocare.dto;

import com.meocare.meocare.entity.HealthUpdate;
import java.time.LocalDateTime;

/** {@link #from(HealthUpdate)} đọc tên bác sĩ (LAZY): cần transaction hoặc fetch trước. */
public record HealthUpdateResponse(
        Long id,
        Long medicalRecordId,
        Long veterinarianId,
        String veterinarianName,
        String healthStatus,
        String description,
        LocalDateTime createdAt
) {
    public static HealthUpdateResponse from(HealthUpdate h) {
        return new HealthUpdateResponse(
                h.getId(),
                h.getMedicalRecord() != null ? h.getMedicalRecord().getId() : null,
                h.getVeterinarian() != null ? h.getVeterinarian().getId() : null,
                h.getVeterinarian() != null ? h.getVeterinarian().getFullName() : null,
                h.getHealthStatus(),
                h.getDescription(),
                h.getCreatedAt()
        );
    }
}
