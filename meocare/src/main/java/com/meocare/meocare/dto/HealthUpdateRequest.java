package com.meocare.meocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** medicalRecordId lấy từ path, veterinarianId lấy từ người dùng đang đăng nhập. */
public record HealthUpdateRequest(
        @NotBlank(message = "Tình trạng sức khỏe không được để trống")
        @Size(max = 30, message = "Tình trạng sức khỏe tối đa 30 ký tự")
        String healthStatus,

        String description,

        List<@NotBlank(message = "URL ảnh không được để trống")
             @Size(max = 500, message = "URL ảnh tối đa 500 ký tự") String> imageUrls
) {
}
