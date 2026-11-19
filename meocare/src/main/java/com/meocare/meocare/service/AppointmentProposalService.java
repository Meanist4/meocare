package com.meocare.meocare.service;

import com.meocare.meocare.dto.ProposalRequest;
import com.meocare.meocare.dto.ProposalResponse;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.AppointmentProposal;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.entity.enums.ProposalStatus;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentProposalRepository;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentProposalService {

    private final AppointmentProposalRepository proposalRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    // ------------------------------------------------------------------ create

    @Transactional
    public ProposalResponse createProposal(ProposalRequest request) {
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lịch hẹn với id " + request.appointmentId()));
        assertAppointmentOpen(appointment);

        User proposedBy = userRepository.findById(request.proposedById())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người đề xuất với id " + request.proposedById()));

        if (request.proposedStart() == null || request.proposedEnd() == null) {
            throw new IllegalArgumentException("Thời gian đề xuất không được để trống");
        }
        if (!request.proposedEnd().isAfter(request.proposedStart())) {
            throw new IllegalArgumentException("Thời gian kết thúc đề xuất phải sau thời gian bắt đầu");
        }

        AppointmentProposal proposal = new AppointmentProposal();
        proposal.setAppointment(appointment);
        proposal.setProposedBy(proposedBy);
        proposal.setProposedStart(request.proposedStart());
        proposal.setProposedEnd(request.proposedEnd());
        proposal.setNote(request.note());
        proposal.setStatus(ProposalStatus.PENDING);

        return toResponse(proposalRepository.save(proposal));
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public List<ProposalResponse> getProposalsByAppointmentId(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException("Không tìm thấy lịch hẹn với id " + appointmentId);
        }
        return proposalRepository.findByAppointment_IdOrderByCreatedAtDesc(appointmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ accept

    @Transactional
    public ProposalResponse acceptProposal(Long proposalId) {
        AppointmentProposal proposal = getEntity(proposalId);
        assertPending(proposal);

        Appointment appointment = proposal.getAppointment();
        assertAppointmentOpen(appointment);

        proposal.setStatus(ProposalStatus.ACCEPTED);

        // Đồng bộ khung giờ đã chốt lên lịch hẹn
        appointment.setConfirmedStart(proposal.getProposedStart());
        appointment.setConfirmedEnd(proposal.getProposedEnd());
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        // Các đề xuất PENDING khác của cùng lịch hẹn không còn hiệu lực
        proposalRepository.findByAppointment_IdAndStatus(appointment.getId(), ProposalStatus.PENDING).stream()
                .filter(other -> !other.getId().equals(proposal.getId()))
                .forEach(other -> other.setStatus(ProposalStatus.DECLINED));

        return toResponse(proposal);
    }

    // ----------------------------------------------------------------- decline

    @Transactional
    public ProposalResponse declineProposal(Long proposalId) {
        AppointmentProposal proposal = getEntity(proposalId);
        assertPending(proposal);
        proposal.setStatus(ProposalStatus.DECLINED);
        return toResponse(proposal);
    }

    // ----------------------------------------------------------------- helpers

    private AppointmentProposal getEntity(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề xuất với id " + id));
    }

    private void assertPending(AppointmentProposal proposal) {
        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new IllegalStateException("Đề xuất đã được xử lý (" + proposal.getStatus() + ")");
        }
    }

    /** Chỉ cho đề xuất/chốt giờ khi lịch hẹn còn PENDING hoặc CONFIRMED (đổi lịch). */
    private void assertAppointmentOpen(Appointment appointment) {
        AppointmentStatus s = appointment.getStatus();
        if (s != AppointmentStatus.PENDING && s != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Lịch hẹn đang ở trạng thái " + s + ", không thể đề xuất/đổi giờ");
        }
    }

    private ProposalResponse toResponse(AppointmentProposal p) {
        return ProposalResponse.from(p);
    }
}
