package com.meocare.meocare.dto;

import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import java.time.LocalDateTime;

/**
 * Lưu ý: {@link #from(Appointment)} đọc tên pet/customer/veterinarian (LAZY).
 * Hãy nạp bằng AppointmentRepository.findDetailById hoặc gọi trong transaction.
 */
public record AppointmentResponse(
        Long id,
        Long petId,
        String petName,
        Long customerId,
        String customerName,
        Long veterinarianId,
        String veterinarianName,
        LocalDateTime requestedStart,
        LocalDateTime requestedEnd,
        LocalDateTime confirmedStart,
        LocalDateTime confirmedEnd,
        String symptoms,
        AppointmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPet() != null ? a.getPet().getId() : null,
                a.getPet() != null ? a.getPet().getName() : null,
                a.getCustomer() != null ? a.getCustomer().getId() : null,
                a.getCustomer() != null ? a.getCustomer().getFullName() : null,
                a.getVeterinarian() != null ? a.getVeterinarian().getId() : null,
                a.getVeterinarian() != null ? a.getVeterinarian().getFullName() : null,
                a.getRequestedStart(),
                a.getRequestedEnd(),
                a.getConfirmedStart(),
                a.getConfirmedEnd(),
                a.getSymptoms(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
