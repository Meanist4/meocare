package com.meocare.meocare.repository;

import com.meocare.meocare.entity.AppointmentProposal;
import com.meocare.meocare.entity.enums.ProposalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentProposalRepository extends JpaRepository<AppointmentProposal, Long> {

    List<AppointmentProposal> findByAppointment_IdOrderByCreatedAtDesc(Long appointmentId);

    List<AppointmentProposal> findByAppointment_IdAndStatus(Long appointmentId, ProposalStatus status);

    Optional<AppointmentProposal> findByIdAndAppointment_Id(Long id, Long appointmentId);

    boolean existsByAppointment_IdAndStatus(Long appointmentId, ProposalStatus status);

    /** Khi một proposal được ACCEPTED: chuyển các proposal PENDING còn lại của lịch sang DECLINED. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AppointmentProposal p
            set p.status = :newStatus
            where p.appointment.id = :appointmentId
              and p.status = :oldStatus
              and p.id <> :exceptId
            """)
    int updateStatusForOthers(@Param("appointmentId") Long appointmentId,
                              @Param("exceptId") Long exceptId,
                              @Param("oldStatus") ProposalStatus oldStatus,
                              @Param("newStatus") ProposalStatus newStatus);
}
