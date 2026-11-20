package com.meocare.meocare.dto;

import com.meocare.meocare.entity.MedicalRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MedicalRecordResponse(
        Long id,
        Long appointmentId,
        Long petId,
        String petName,
        Long veterinarianId,
        String veterinarianName,
        BigDecimal weight,
        BigDecimal temperature,
        String examination,
        String diagnosis,
        String treatment,
        String advice,
        String notes,
        List<MedicalRecordImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MedicalRecordResponse from(MedicalRecord m) {
        return from(m, List.of());
    }

    public static MedicalRecordResponse from(MedicalRecord m, List<MedicalRecordImageResponse> images) {
        return new MedicalRecordResponse(
                m.getId(),
                m.getAppointment() != null ? m.getAppointment().getId() : null,
                m.getPet() != null ? m.getPet().getId() : null,
                m.getPet() != null ? m.getPet().getName() : null,
                m.getVeterinarian() != null ? m.getVeterinarian().getId() : null,
                m.getVeterinarian() != null ? m.getVeterinarian().getFullName() : null,
                m.getWeight(),
                m.getTemperature(),
                m.getExamination(),
                m.getDiagnosis(),
                m.getTreatment(),
                m.getAdvice(),
                m.getNotes(),
                images != null ? images : List.of(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
