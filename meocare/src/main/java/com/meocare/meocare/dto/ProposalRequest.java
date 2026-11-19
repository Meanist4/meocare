package com.meocare.meocare.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** appointmentId lấy từ path, proposedBy lấy từ người dùng đang đăng nhập. */
public record ProposalRequest(
        @NotNull(message = "appointmentId không được để trống")
        Long appointmentId,

        @NotNull(message = "proposedById không được để trống")
        Long proposedById,

        @NotNull(message = "Thời gian bắt đầu đề xuất không được để trống")
        @Future(message = "Thời gian bắt đầu đề xuất phải ở tương lai")
        LocalDateTime proposedStart,

        @NotNull(message = "Thời gian kết thúc đề xuất không được để trống")
        @Future(message = "Thời gian kết thúc đề xuất phải ở tương lai")
        LocalDateTime proposedEnd,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
    @AssertTrue(message = "Thời gian kết thúc đề xuất phải sau thời gian bắt đầu")
    public boolean isProposedTimeRangeValid() {
        return proposedStart == null || proposedEnd == null || proposedEnd.isAfter(proposedStart);
    }
}
