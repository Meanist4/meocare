package com.meocare.meocare.dto;

import com.meocare.meocare.entity.AppointmentImage;
import java.time.LocalDateTime;

public record AppointmentImageResponse(
        Long id,
        Long appointmentId,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static AppointmentImageResponse from(AppointmentImage image) {
        return new AppointmentImageResponse(
                image.getId(),
                image.getAppointment() != null ? image.getAppointment().getId() : null,
                image.getImageUrl(),
                image.getCreatedAt()
        );
    }
}
