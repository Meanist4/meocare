package com.meocare.meocare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record MedicalRecordUpdateRequest(
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
        String notes
) {
}
