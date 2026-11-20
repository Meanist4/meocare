package com.meocare.meocare.service;

import com.meocare.meocare.dto.ProposalRequest;
import com.meocare.meocare.dto.ProposalResponse;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.AppointmentProposal;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.entity.enums.ProposalStatus;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ConflictException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentProposalRepository;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.UserRepository;
import com.meocare.meocare.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentProposalService {

    private final AppointmentProposalRepository proposalRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentService appointmentService;

    @Transactional
    public ProposalResponse createProposal(Long pathAppointmentId,
                                           ProposalRequest request,
                                           CustomUserDetails principal) {
        Long appointmentId = pathAppointmentId != null ? pathAppointmentId : request.appointmentId();
        if (appointmentId == null) {
            throw new BadRequestException("appointmentId is required");
        }
        if (pathAppointmentId != null && request.appointmentId() != null
                && !pathAppointmentId.equals(request.appointmentId())) {
            throw new BadRequestException("Path appointmentId does not match request body appointmentId");
        }

        Appointment appointment = appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        appointmentService.assertViewAccess(appointment, principal);
        assertAppointmentNegotiable(appointment);

        if (request.proposedStart() == null || request.proposedEnd() == null) {
            throw new BadRequestException("Proposed start and end times are required");
        }
        if (!request.proposedEnd().isAfter(request.proposedStart())) {
            throw new BadRequestException("Proposed end time must be after proposed start time");
        }

        User proposedBy = userRepository.findWithRoleById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        if (principal.getRole() == RoleName.VETERINARIAN && appointment.getVeterinarian() == null) {
            appointment.setVeterinarian(proposedBy);
        }

        AppointmentProposal proposal = new AppointmentProposal();
        proposal.setAppointment(appointment);
        proposal.setProposedBy(proposedBy);
        proposal.setProposedStart(request.proposedStart());
        proposal.setProposedEnd(request.proposedEnd());
        proposal.setNote(request.note());
        proposal.setStatus(ProposalStatus.PENDING);

        appointment.setStatus(AppointmentStatus.RESCHEDULE_PROPOSED);
        appointmentRepository.save(appointment);

        return ProposalResponse.from(proposalRepository.save(proposal));
    }

    public List<ProposalResponse> getProposalsByAppointmentId(Long appointmentId, CustomUserDetails principal) {
        Appointment appointment = appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        appointmentService.assertViewAccess(appointment, principal);

        return proposalRepository.findByAppointment_IdOrderByCreatedAtDesc(appointmentId).stream()
                .map(ProposalResponse::from)
                .toList();
    }

    @Transactional
    public ProposalResponse acceptProposal(Long pathAppointmentId, Long proposalId, CustomUserDetails principal) {
        AppointmentProposal proposal = getProposalDetail(pathAppointmentId, proposalId);
        assertPending(proposal);

        Appointment appointment = proposal.getAppointment();
        appointmentService.assertViewAccess(appointment, principal);
        assertAppointmentNegotiable(appointment);

        if (proposal.getProposedBy() != null && proposal.getProposedBy().getId().equals(principal.getId())) {
            throw new AccessDeniedException(
                    "You cannot accept your own reschedule proposal; the counterparty or an administrator must respond");
        }

        if (principal.getRole() == RoleName.VETERINARIAN && appointment.getVeterinarian() == null) {
            appointment.setVeterinarian(appointmentService.findActiveVeterinarian(principal.getId()));
        }

        if (appointment.getVeterinarian() != null) {
            appointmentService.assertNoVeterinarianConflict(
                    appointment.getVeterinarian().getId(),
                    proposal.getProposedStart(),
                    proposal.getProposedEnd(),
                    appointment.getId()
            );
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);

        appointment.setConfirmedStart(proposal.getProposedStart());
        appointment.setConfirmedEnd(proposal.getProposedEnd());
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        proposalRepository.findByAppointment_IdAndStatus(appointment.getId(), ProposalStatus.PENDING).stream()
                .filter(other -> !other.getId().equals(proposal.getId()))
                .forEach(other -> other.setStatus(ProposalStatus.DECLINED));

        return ProposalResponse.from(proposalRepository.save(proposal));
    }

    @Transactional
    public ProposalResponse declineProposal(Long pathAppointmentId, Long proposalId, CustomUserDetails principal) {
        AppointmentProposal proposal = getProposalDetail(pathAppointmentId, proposalId);
        assertPending(proposal);

        Appointment appointment = proposal.getAppointment();
        appointmentService.assertViewAccess(appointment, principal);

        proposal.setStatus(ProposalStatus.DECLINED);
        return ProposalResponse.from(proposalRepository.save(proposal));
    }

    private AppointmentProposal getProposalDetail(Long pathAppointmentId, Long proposalId) {
        AppointmentProposal proposal = proposalRepository.findDetailById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment proposal", proposalId));
        if (pathAppointmentId != null
                && (proposal.getAppointment() == null
                || !pathAppointmentId.equals(proposal.getAppointment().getId()))) {
            throw new ResourceNotFoundException("Appointment proposal", proposalId);
        }
        return proposal;
    }

    private void assertPending(AppointmentProposal proposal) {
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new ConflictException("Proposal has already been processed (" + proposal.getStatus() + ")");
        }
    }

    private void assertAppointmentNegotiable(Appointment appointment) {
        AppointmentStatus status = appointment.getStatus();
        if (status != AppointmentStatus.PENDING
                && status != AppointmentStatus.RESCHEDULE_PROPOSED
                && status != AppointmentStatus.CONFIRMED) {
            throw new ConflictException(
                    "Appointment in status " + status + " cannot accept reschedule proposals");
        }
    }
}
