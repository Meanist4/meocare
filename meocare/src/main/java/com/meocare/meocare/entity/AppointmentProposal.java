package com.meocare.meocare.entity;

import java.time.LocalDateTime;
import com.meocare.meocare.entity.enums.ProposalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "appointment_proposals")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by")
    private User proposedBy;

    @Column(name = "proposed_start")
    private LocalDateTime proposedStart;

    @Column(name = "proposed_end")
    private LocalDateTime proposedEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ProposalStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
