package com.meocare.meocare.dto;

import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Appointment summary/detail DTO including attached symptom images.
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
        List<AppointmentImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppointmentResponse from(Appointment a) {
        return from(a, List.of());
    }

    public static AppointmentResponse from(Appointment a, List<AppointmentImageResponse> images) {
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
                images != null ? images : List.of(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
