package com.meocare.meocare.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id")
    private User veterinarian;

    @Column(name = "requested_start")
    private LocalDateTime requestedStart;

    @Column(name = "requested_end")
    private LocalDateTime requestedEnd;

    @Column(name = "confirmed_start")
    private LocalDateTime confirmedStart;

    @Column(name = "confirmed_end")
    private LocalDateTime confirmedEnd;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private AppointmentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY)
    private List<AppointmentProposal> proposals = new ArrayList<>();

    @OneToMany(mappedBy = "appointment", fetch = FetchType.LAZY)
    private List<AppointmentImage> images = new ArrayList<>();

    @OneToOne(mappedBy = "appointment", fetch = FetchType.LAZY)
    private MedicalRecord medicalRecord;

}
