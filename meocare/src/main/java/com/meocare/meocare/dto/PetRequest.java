package com.meocare.meocare.dto;

import com.meocare.meocare.entity.enums.PetGender;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank(message = "Tên thú cưng không được để trống")
        @Size(max = 100, message = "Tên thú cưng tối đa 100 ký tự")
        String name,

        @NotBlank(message = "Loài không được để trống")
        @Size(max = 50, message = "Loài tối đa 50 ký tự")
        String species,

        @Size(max = 100, message = "Giống tối đa 100 ký tự")
        String breed,

        PetGender gender,

        @PastOrPresent(message = "Ngày sinh không được ở tương lai")
        LocalDate dateOfBirth,

        @PositiveOrZero(message = "Cân nặng không được âm")
        @Digits(integer = 3, fraction = 2, message = "Cân nặng tối đa 3 chữ số nguyên và 2 chữ số thập phân")
        BigDecimal weight,

        String description
) {
}
