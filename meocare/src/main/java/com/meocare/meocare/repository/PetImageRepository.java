package com.meocare.meocare.repository;

import com.meocare.meocare.entity.PetImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetImageRepository extends JpaRepository<PetImage, Long> {

    List<PetImage> findByPet_IdOrderByCreatedAtDesc(Long petId);

    List<PetImage> findByPet_IdInOrderByCreatedAtDesc(Collection<Long> petIds);

    Optional<PetImage> findByIdAndPet_Id(Long id, Long petId);

    long countByPet_Id(Long petId);
}
