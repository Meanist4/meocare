package com.meocare.meocare.repository;

import com.meocare.meocare.entity.AppointmentProposal;
import com.meocare.meocare.entity.enums.ProposalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentProposalRepository extends JpaRepository<AppointmentProposal, Long> {

    @EntityGraph(attributePaths = {"proposedBy", "proposedBy.role", "appointment"})
    List<AppointmentProposal> findByAppointment_IdOrderByCreatedAtDesc(Long appointmentId);

    List<AppointmentProposal> findByAppointment_IdAndStatus(Long appointmentId, ProposalStatus status);

    Optional<AppointmentProposal> findByIdAndAppointment_Id(Long id, Long appointmentId);

    @EntityGraph(attributePaths = {
            "proposedBy",
            "proposedBy.role",
            "appointment",
            "appointment.customer",
            "appointment.veterinarian"
    })
    @Query("select p from AppointmentProposal p where p.id = :id")
    Optional<AppointmentProposal> findDetailById(@Param("id") Long id);

    boolean existsByAppointment_IdAndStatus(Long appointmentId, ProposalStatus status);

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AppointmentProposal p
            set p.status = :newStatus
            where p.appointment.id = :appointmentId
              and p.status = :oldStatus
            """)
    int updateAllStatusByAppointment(@Param("appointmentId") Long appointmentId,
                                     @Param("oldStatus") ProposalStatus oldStatus,
                                     @Param("newStatus") ProposalStatus newStatus);
}
