package com.meocare.meocare.dto;

import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.enums.PetGender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PetResponse(
        Long id,
        Long ownerId,
        String ownerName,
        String name,
        String species,
        String breed,
        PetGender gender,
        LocalDate dateOfBirth,
        BigDecimal weight,
        String description,
        List<PetImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PetResponse from(Pet pet) {
        return from(pet, List.of());
    }

    public static PetResponse from(Pet pet, List<PetImageResponse> images) {
        return new PetResponse(
                pet.getId(),
                pet.getOwner() != null ? pet.getOwner().getId() : null,
                pet.getOwner() != null ? pet.getOwner().getFullName() : null,
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getGender(),
                pet.getDateOfBirth(),
                pet.getWeight(),
                pet.getDescription(),
                images != null ? images : List.of(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}
