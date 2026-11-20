package com.meocare.meocare.repository;

import com.meocare.meocare.entity.MedicalRecordImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordImageRepository extends JpaRepository<MedicalRecordImage, Long> {

    List<MedicalRecordImage> findByMedicalRecord_IdOrderByCreatedAtDesc(Long medicalRecordId);

    List<MedicalRecordImage> findByMedicalRecord_IdInOrderByCreatedAtDesc(Collection<Long> medicalRecordIds);

    Optional<MedicalRecordImage> findByIdAndMedicalRecord_Id(Long id, Long medicalRecordId);

    long countByMedicalRecord_Id(Long medicalRecordId);
}
