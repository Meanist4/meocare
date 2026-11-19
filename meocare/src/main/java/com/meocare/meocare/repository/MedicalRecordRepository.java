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

    /** Kiểm tra quyền xem của chủ thú cưng. */
    boolean existsByIdAndPet_Owner_Id(Long id, Long ownerId);

    /** Hồ sơ của tất cả thú cưng thuộc một khách hàng. */
    List<MedicalRecord> findByPet_Owner_IdOrderByCreatedAtDesc(Long ownerId);

    @Query("""
            select m from MedicalRecord m
            join fetch m.pet
            join fetch m.veterinarian
            join fetch m.appointment
            where m.id = :id
            """)
    Optional<MedicalRecord> findDetailById(@Param("id") Long id);
}
