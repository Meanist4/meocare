package com.meocare.meocare.repository;

import com.meocare.meocare.entity.MedicalRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Optional<MedicalRecord> findByAppointment_Id(Long appointmentId);

    boolean existsByAppointment_Id(Long appointmentId);

    List<MedicalRecord> findByPet_IdOrderByCreatedAtDesc(Long petId);

    List<MedicalRecord> findByVeterinarian_IdOrderByCreatedAtDesc(Long veterinarianId);

    boolean existsByIdAndPet_Owner_Id(Long id, Long ownerId);

    List<MedicalRecord> findByPet_Owner_IdOrderByCreatedAtDesc(Long ownerId);

    @Query("""
            select m from MedicalRecord m
            join fetch m.pet p
            join fetch p.owner
            join fetch m.veterinarian
            join fetch m.appointment
            where m.id = :id
            """)
    Optional<MedicalRecord> findDetailById(@Param("id") Long id);

    @Query("""
            select m from MedicalRecord m
            join fetch m.pet p
            join fetch p.owner
            join fetch m.veterinarian
            join fetch m.appointment
            where m.appointment.id = :appointmentId
            """)
    Optional<MedicalRecord> findDetailByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Query("""
            select m from MedicalRecord m
            join fetch m.pet p
            join fetch p.owner
            join fetch m.veterinarian
            join fetch m.appointment
            where (:petId is null or p.id = :petId)
              and (:veterinarianId is null or m.veterinarian.id = :veterinarianId)
              and (:customerId is null or p.owner.id = :customerId)
            order by m.createdAt desc
            """)
    List<MedicalRecord> findFiltered(@Param("petId") Long petId,
                                     @Param("veterinarianId") Long veterinarianId,
                                     @Param("customerId") Long customerId);
}
