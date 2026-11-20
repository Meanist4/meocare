package com.meocare.meocare.repository;

import com.meocare.meocare.entity.Pet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @EntityGraph(attributePaths = "owner")
    List<Pet> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "owner")
    List<Pet> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = "owner")
    Optional<Pet> findWithOwnerById(Long id);

    Optional<Pet> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByIdAndOwner_Id(Long id, Long ownerId);
}
