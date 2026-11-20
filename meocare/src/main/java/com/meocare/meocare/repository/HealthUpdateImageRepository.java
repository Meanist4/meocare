package com.meocare.meocare.repository;

import com.meocare.meocare.entity.HealthUpdateImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthUpdateImageRepository extends JpaRepository<HealthUpdateImage, Long> {

    List<HealthUpdateImage> findByHealthUpdate_IdOrderByCreatedAtDesc(Long healthUpdateId);

    List<HealthUpdateImage> findByHealthUpdate_IdInOrderByCreatedAtDesc(Collection<Long> healthUpdateIds);

    Optional<HealthUpdateImage> findByIdAndHealthUpdate_Id(Long id, Long healthUpdateId);

    long countByHealthUpdate_Id(Long healthUpdateId);
}
