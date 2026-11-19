package com.meocare.meocare.dto;

import com.meocare.meocare.entity.enums.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public record AppointmentStatusUpdateRequest(@NotNull AppointmentStatus status) {
}
