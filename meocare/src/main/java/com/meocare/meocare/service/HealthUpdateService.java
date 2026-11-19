package com.meocare.meocare.service;

import com.meocare.meocare.dto.HealthUpdateImageResponse;
import com.meocare.meocare.dto.HealthUpdateRequest;
import com.meocare.meocare.dto.HealthUpdateResponse;
import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.entity.HealthUpdate;
import com.meocare.meocare.entity.HealthUpdateImage;
import com.meocare.meocare.entity.MedicalRecord;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.HealthUpdateImageRepository;
import com.meocare.meocare.repository.HealthUpdateRepository;
import com.meocare.meocare.repository.MedicalRecordRepository;
import com.meocare.meocare.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthUpdateService {

    private final HealthUpdateRepository healthUpdateRepository;
    private final HealthUpdateImageRepository imageRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public HealthUpdateResponse createHealthUpdate(Long medicalRecordId, HealthUpdateRequest request) {
        MedicalRecord medicalRecord = medicalRecordRepository.findDetailById(medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ bệnh án", medicalRecordId));
        if (medicalRecord.getVeterinarian() == null) {
            throw new BadRequestException("Hồ sơ bệnh án chưa được gán bác sĩ thú y");
        }
        User veterinarian = userRepository.findById(medicalRecord.getVeterinarian().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bác sĩ thú y của hồ sơ bệnh án", medicalRecord.getVeterinarian().getId()));

        HealthUpdate healthUpdate = new HealthUpdate();
        healthUpdate.setMedicalRecord(medicalRecord);
        healthUpdate.setVeterinarian(veterinarian);
        healthUpdate.setHealthStatus(request.healthStatus().trim());
        healthUpdate.setDescription(request.description());

        HealthUpdate saved = healthUpdateRepository.save(healthUpdate);
        saveImages(saved, request.imageUrls());
        return HealthUpdateResponse.from(saved);
    }

    public List<HealthUpdateResponse> getHealthUpdatesByMedicalRecordId(Long medicalRecordId) {
        if (!medicalRecordRepository.existsById(medicalRecordId)) {
            throw new ResourceNotFoundException("Hồ sơ bệnh án", medicalRecordId);
        }
        return healthUpdateRepository.findByMedicalRecord_IdOrderByCreatedAtDesc(medicalRecordId).stream()
                .map(HealthUpdateResponse::from)
                .toList();
    }

    @Transactional
    public List<HealthUpdateImageResponse> addImages(Long healthUpdateId, ImageUrlsRequest request) {
        HealthUpdate healthUpdate = healthUpdateRepository.findById(healthUpdateId)
                .orElseThrow(() -> new ResourceNotFoundException("Cập nhật sức khỏe", healthUpdateId));
        List<HealthUpdateImage> images = request.imageUrls().stream()
                .map(url -> createImage(healthUpdate, url))
                .toList();
        return imageRepository.saveAll(images).stream()
                .map(HealthUpdateImageResponse::from)
                .toList();
    }

    public List<HealthUpdateImageResponse> getImages(Long healthUpdateId) {
        if (!healthUpdateRepository.existsById(healthUpdateId)) {
            throw new ResourceNotFoundException("Cập nhật sức khỏe", healthUpdateId);
        }
        return imageRepository.findByHealthUpdate_IdOrderByCreatedAtDesc(healthUpdateId).stream()
                .map(HealthUpdateImageResponse::from)
                .toList();
    }

    private void saveImages(HealthUpdate healthUpdate, List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imageRepository.saveAll(imageUrls.stream()
                    .map(url -> createImage(healthUpdate, url))
                    .toList());
        }
    }

    private HealthUpdateImage createImage(HealthUpdate healthUpdate, String imageUrl) {
        HealthUpdateImage image = new HealthUpdateImage();
        image.setHealthUpdate(healthUpdate);
        image.setImageUrl(imageUrl.trim());
        return image;
    }
}
