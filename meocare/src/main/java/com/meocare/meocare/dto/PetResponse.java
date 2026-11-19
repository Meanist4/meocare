package com.meocare.meocare.dto;

import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.enums.PetGender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetResponse(
        Long id,
        Long ownerId,
        String name,
        String species,
        String breed,
        PetGender gender,
        LocalDate dateOfBirth,
        BigDecimal weight,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getOwner() != null ? pet.getOwner().getId() : null,
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getGender(),
                pet.getDateOfBirth(),
                pet.getWeight(),
                pet.getDescription(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}
