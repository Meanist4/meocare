package com.meocare.meocare.repository;

import com.meocare.meocare.entity.MedicalRecordImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordImageRepository extends JpaRepository<MedicalRecordImage, Long> {

    List<MedicalRecordImage> findByMedicalRecord_IdOrderByCreatedAtDesc(Long medicalRecordId);

    Optional<MedicalRecordImage> findByIdAndMedicalRecord_Id(Long id, Long medicalRecordId);

    long countByMedicalRecord_Id(Long medicalRecordId);
}
