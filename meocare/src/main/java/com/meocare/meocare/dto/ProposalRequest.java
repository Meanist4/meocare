package com.meocare.meocare.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Reschedule proposal payload.
 * proposedBy is always derived from the authenticated JWT principal.
 * appointmentId is required for POST /api/appointment-proposals and optional when supplied via path
 * in POST /api/appointments/{appointmentId}/proposals.
 */
public record ProposalRequest(
        Long appointmentId,

        @NotNull(message = "Proposed start time is required")
        @Future(message = "Proposed start time must be in the future")
        LocalDateTime proposedStart,

        @NotNull(message = "Proposed end time is required")
        @Future(message = "Proposed end time must be in the future")
        LocalDateTime proposedEnd,

        @Size(max = 500, message = "Note must be at most 500 characters")
        String note
) {
    @AssertTrue(message = "Proposed end time must be after proposed start time")
    public boolean isProposedTimeRangeValid() {
        return proposedStart == null || proposedEnd == null || proposedEnd.isAfter(proposedStart);
    }
}
