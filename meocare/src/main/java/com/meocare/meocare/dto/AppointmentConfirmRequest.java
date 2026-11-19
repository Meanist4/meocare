package com.meocare.meocare.dto;

import java.time.LocalDateTime;

/**
 * Body tuỳ chọn cho PATCH /api/appointments/{id}/confirm.
 * Bỏ trống toàn bộ => chốt đúng khung giờ khách yêu cầu (requestedStart/requestedEnd).
 * Nếu truyền thì phải truyền cả confirmedStart và confirmedEnd.
 */
public record AppointmentConfirmRequest(
        LocalDateTime confirmedStart,
        LocalDateTime confirmedEnd,
        Long veterinarianId) {
}
