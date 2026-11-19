package com.meocare.meocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PetImageRequest(
        @NotBlank(message = "imageUrl không được để trống")
        @Size(max = 500, message = "imageUrl tối đa 500 ký tự")
        String imageUrl
) {
}
