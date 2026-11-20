package com.meocare.meocare.repository;

import com.meocare.meocare.entity.HealthUpdate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthUpdateRepository extends JpaRepository<HealthUpdate, Long> {

    List<HealthUpdate> findByMedicalRecord_IdOrderByCreatedAtDesc(Long medicalRecordId);

    Optional<HealthUpdate> findFirstByMedicalRecord_IdOrderByCreatedAtDesc(Long medicalRecordId);

    Optional<HealthUpdate> findByIdAndMedicalRecord_Id(Long id, Long medicalRecordId);

    boolean existsByIdAndMedicalRecord_Pet_Owner_Id(Long id, Long ownerId);

    @Query("""
            select h from HealthUpdate h
            join fetch h.medicalRecord mr
            join fetch mr.pet p
            join fetch p.owner
            join fetch h.veterinarian
            where h.id = :id
            """)
    Optional<HealthUpdate> findDetailById(@Param("id") Long id);

    @Query("""
            select h from HealthUpdate h
            join fetch h.medicalRecord mr
            join fetch mr.pet p
            join fetch p.owner
            join fetch h.veterinarian
            where (:petId is null or p.id = :petId)
              and (:customerId is null or p.owner.id = :customerId)
              and (:medicalRecordId is null or mr.id = :medicalRecordId)
            order by h.createdAt desc
            """)
    List<HealthUpdate> findTimeline(@Param("petId") Long petId,
                                    @Param("customerId") Long customerId,
                                    @Param("medicalRecordId") Long medicalRecordId);
}
