package com.meocare.meocare.repository;

import com.meocare.meocare.entity.HealthUpdate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthUpdateRepository extends JpaRepository<HealthUpdate, Long> {

    List<HealthUpdate> findByMedicalRecord_IdOrderByCreatedAtDesc(Long medicalRecordId);

    Optional<HealthUpdate> findFirstByMedicalRecord_IdOrderByCreatedAtDesc(Long medicalRecordId);

    Optional<HealthUpdate> findByIdAndMedicalRecord_Id(Long id, Long medicalRecordId);

    /** Kiểm tra quyền xem của chủ thú cưng. */
    boolean existsByIdAndMedicalRecord_Pet_Owner_Id(Long id, Long ownerId);
}
