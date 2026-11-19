package com.meocare.meocare.repository;

import com.meocare.meocare.entity.Pet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    Optional<Pet> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByIdAndOwner_Id(Long id, Long ownerId);
}
