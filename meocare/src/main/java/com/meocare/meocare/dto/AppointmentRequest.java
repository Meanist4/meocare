package com.meocare.meocare.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * customerId lấy từ người dùng đang đăng nhập, không nhận từ client.
 * veterinarianId là tùy chọn (khách có thể chọn sẵn bác sĩ).
 */
public record AppointmentRequest(
        @NotNull(message = "petId không được để trống")
        Long petId,

        Long veterinarianId,

        @NotNull(message = "Thời gian bắt đầu mong muốn không được để trống")
        @Future(message = "Thời gian bắt đầu phải ở tương lai")
        LocalDateTime requestedStart,

        @NotNull(message = "Thời gian kết thúc mong muốn không được để trống")
        @Future(message = "Thời gian kết thúc phải ở tương lai")
        LocalDateTime requestedEnd,

        String symptoms,

        List<@NotBlank(message = "URL ảnh không được để trống")
             @Size(max = 500, message = "URL ảnh tối đa 500 ký tự") String> imageUrls
) {
    @AssertTrue(message = "Thời gian kết thúc phải sau thời gian bắt đầu")
    public boolean isRequestedTimeRangeValid() {
        return requestedStart == null || requestedEnd == null || requestedEnd.isAfter(requestedStart);
    }
}
