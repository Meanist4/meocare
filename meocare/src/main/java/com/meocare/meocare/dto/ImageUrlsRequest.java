package com.meocare.meocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ImageUrlsRequest(
        @NotEmpty(message = "Danh sách URL ảnh không được để trống")
        List<
                @NotBlank(message = "URL ảnh không được để trống")
                @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
                String> imageUrls
) {
}
