package com.meocare.meocare.dto;

import com.meocare.meocare.entity.AppointmentProposal;
import com.meocare.meocare.entity.enums.ProposalStatus;
import com.meocare.meocare.entity.enums.RoleName;
import java.time.LocalDateTime;

public record ProposalResponse(
        Long id,
        Long appointmentId,
        Long proposedById,
        String proposedByName,
        RoleName proposedByRole,
        LocalDateTime proposedStart,
        LocalDateTime proposedEnd,
        ProposalStatus status,
        String note,
        LocalDateTime createdAt
) {
    public static ProposalResponse from(AppointmentProposal p) {
        RoleName role = (p.getProposedBy() != null && p.getProposedBy().getRole() != null)
                ? p.getProposedBy().getRole().getName()
                : null;
        return new ProposalResponse(
                p.getId(),
                p.getAppointment() != null ? p.getAppointment().getId() : null,
                p.getProposedBy() != null ? p.getProposedBy().getId() : null,
                p.getProposedBy() != null ? p.getProposedBy().getFullName() : null,
                role,
                p.getProposedStart(),
                p.getProposedEnd(),
                p.getStatus(),
                p.getNote(),
                p.getCreatedAt()
        );
    }
}
