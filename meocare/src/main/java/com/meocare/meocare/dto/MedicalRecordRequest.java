package com.meocare.meocare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** petId lấy từ appointment, veterinarianId lấy từ người dùng đang đăng nhập. */
public record MedicalRecordRequest(
        @NotNull(message = "appointmentId không được để trống")
        Long appointmentId,

        @PositiveOrZero(message = "Cân nặng không được âm")
        @Digits(integer = 3, fraction = 2, message = "Cân nặng tối đa 3 chữ số nguyên và 2 chữ số thập phân")
        BigDecimal weight,

        @DecimalMin(value = "30.0", message = "Nhiệt độ tối thiểu 30.0")
        @DecimalMax(value = "45.0", message = "Nhiệt độ tối đa 45.0")
        @Digits(integer = 3, fraction = 1, message = "Nhiệt độ tối đa 1 chữ số thập phân")
        BigDecimal temperature,

        String examination,
        String diagnosis,
        String treatment,
        String advice,
        String notes,

        List<@NotBlank(message = "URL ảnh không được để trống")
             @Size(max = 500, message = "URL ảnh tối đa 500 ký tự") String> imageUrls
) {
}
