package com.meocare.meocare.service;

import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.dto.MedicalRecordImageResponse;
import com.meocare.meocare.dto.MedicalRecordRequest;
import com.meocare.meocare.dto.MedicalRecordResponse;
import com.meocare.meocare.dto.MedicalRecordUpdateRequest;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.MedicalRecord;
import com.meocare.meocare.entity.MedicalRecordImage;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.MedicalRecordImageRepository;
import com.meocare.meocare.repository.MedicalRecordRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordImageRepository imageRepository;
    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Transactional
    public MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request) {
        Appointment appointment = appointmentRepository.findDetailById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn", request.appointmentId()));

        if (medicalRecordRepository.existsByAppointment_Id(request.appointmentId())) {
            throw new BadRequestException(
                    "Lịch hẹn " + request.appointmentId() + " đã có hồ sơ bệnh án");
        }
        if (appointment.getPet() == null) {
            throw new BadRequestException("Lịch hẹn chưa được gắn với thú cưng");
        }
        Pet pet = petRepository.findById(appointment.getPet().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Thú cưng của lịch hẹn", appointment.getPet().getId()));

        if (appointment.getVeterinarian() == null) {
            throw new BadRequestException("Lịch hẹn chưa được gán bác sĩ thú y");
        }
        User veterinarian = userRepository.findById(appointment.getVeterinarian().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bác sĩ thú y của lịch hẹn", appointment.getVeterinarian().getId()));

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setPet(pet);
        record.setVeterinarian(veterinarian);
        applyFields(record, request.weight(), request.temperature(), request.examination(),
                request.diagnosis(), request.treatment(), request.advice(), request.notes());

        MedicalRecord saved = medicalRecordRepository.save(record);
        saveImages(saved, request.imageUrls());
        return MedicalRecordResponse.from(saved);
    }

    public MedicalRecordResponse getMedicalRecordById(Long id) {
        return MedicalRecordResponse.from(findRecord(id));
    }

    public MedicalRecordResponse getMedicalRecordByAppointmentId(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException("Lịch hẹn", appointmentId);
        }
        MedicalRecord record = medicalRecordRepository.findByAppointment_Id(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Hồ sơ bệnh án của lịch hẹn", appointmentId));
        return MedicalRecordResponse.from(record);
    }

    public List<MedicalRecordResponse> getMedicalRecordsByPetId(Long petId) {
        if (!petRepository.existsById(petId)) {
            throw new ResourceNotFoundException("Thú cưng", petId);
        }
        return medicalRecordRepository.findByPet_IdOrderByCreatedAtDesc(petId).stream()
                .map(MedicalRecordResponse::from)
                .toList();
    }

    @Transactional
    public MedicalRecordResponse updateMedicalRecord(Long id, MedicalRecordUpdateRequest request) {
        MedicalRecord record = findRecord(id);
        applyFields(record, request.weight(), request.temperature(), request.examination(),
                request.diagnosis(), request.treatment(), request.advice(), request.notes());
        return MedicalRecordResponse.from(medicalRecordRepository.save(record));
    }

    @Transactional
    public List<MedicalRecordImageResponse> addImages(Long medicalRecordId, ImageUrlsRequest request) {
        MedicalRecord record = findRecord(medicalRecordId);
        List<MedicalRecordImage> images = request.imageUrls().stream()
                .map(url -> createImage(record, url))
                .toList();
        return imageRepository.saveAll(images).stream()
                .map(MedicalRecordImageResponse::from)
                .toList();
    }

    public List<MedicalRecordImageResponse> getImages(Long medicalRecordId) {
        if (!medicalRecordRepository.existsById(medicalRecordId)) {
            throw new ResourceNotFoundException("Hồ sơ bệnh án", medicalRecordId);
        }
        return imageRepository.findByMedicalRecord_IdOrderByCreatedAtDesc(medicalRecordId).stream()
                .map(MedicalRecordImageResponse::from)
                .toList();
    }

    private MedicalRecord findRecord(Long id) {
        return medicalRecordRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ bệnh án", id));
    }

    private void saveImages(MedicalRecord record, List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imageRepository.saveAll(imageUrls.stream()
                    .map(url -> createImage(record, url))
                    .toList());
        }
    }

    private MedicalRecordImage createImage(MedicalRecord record, String imageUrl) {
        MedicalRecordImage image = new MedicalRecordImage();
        image.setMedicalRecord(record);
        image.setImageUrl(imageUrl.trim());
        return image;
    }

    private void applyFields(MedicalRecord record, BigDecimal weight,
                             BigDecimal temperature, String examination,
                             String diagnosis, String treatment, String advice, String notes) {
        record.setWeight(weight);
        record.setTemperature(temperature);
        record.setExamination(examination);
        record.setDiagnosis(diagnosis);
        record.setTreatment(treatment);
        record.setAdvice(advice);
        record.setNotes(notes);
    }
}
