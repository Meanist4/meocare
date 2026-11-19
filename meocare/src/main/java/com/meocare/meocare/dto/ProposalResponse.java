package com.meocare.meocare.dto;

import com.meocare.meocare.entity.AppointmentProposal;
import com.meocare.meocare.entity.enums.ProposalStatus;
import java.time.LocalDateTime;

/** {@link #from(AppointmentProposal)} đọc tên người đề xuất (LAZY): cần transaction hoặc fetch trước. */
public record ProposalResponse(
        Long id,
        Long appointmentId,
        Long proposedById,
        String proposedByName,
        LocalDateTime proposedStart,
        LocalDateTime proposedEnd,
        ProposalStatus status,
        String note,
        LocalDateTime createdAt
) {
    public static ProposalResponse from(AppointmentProposal p) {
        return new ProposalResponse(
                p.getId(),
                p.getAppointment() != null ? p.getAppointment().getId() : null,
                p.getProposedBy() != null ? p.getProposedBy().getId() : null,
                p.getProposedBy() != null ? p.getProposedBy().getFullName() : null,
                p.getProposedStart(),
                p.getProposedEnd(),
                p.getStatus(),
                p.getNote(),
                p.getCreatedAt()
        );
    }
}
