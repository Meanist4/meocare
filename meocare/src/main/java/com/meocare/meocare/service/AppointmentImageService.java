package com.meocare.meocare.service;

import com.meocare.meocare.dto.AppointmentImageResponse;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.AppointmentImage;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentImageRepository;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.security.CustomUserDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentImageService {

    public record ImageRequest(
            @NotBlank(message = "imageUrl is required")
            @Size(max = 500, message = "imageUrl must be at most 500 characters")
            String imageUrl
    ) {
    }

    private final AppointmentImageRepository imageRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;

    @Transactional
    public AppointmentImageResponse addImage(Long appointmentId, ImageRequest request, CustomUserDetails principal) {
        Appointment appointment = appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        assertWriteAccess(appointment, principal);

        AppointmentImage image = new AppointmentImage();
        image.setAppointment(appointment);
        image.setImageUrl(request.imageUrl().trim());
        return AppointmentImageResponse.from(imageRepository.save(image));
    }

    public List<AppointmentImageResponse> getImages(Long appointmentId, CustomUserDetails principal) {
        Appointment appointment = appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        appointmentService.assertViewAccess(appointment, principal);

        return imageRepository.findByAppointment_IdOrderByCreatedAtDesc(appointmentId).stream()
                .map(AppointmentImageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteImage(Long appointmentId, Long imageId, CustomUserDetails principal) {
        Appointment appointment = appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        assertWriteAccess(appointment, principal);

        AppointmentImage image = imageRepository.findByIdAndAppointment_Id(imageId, appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment image", imageId));
        imageRepository.delete(image);
    }

    private void assertWriteAccess(Appointment appointment, CustomUserDetails principal) {
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() == RoleName.CUSTOMER
                && appointment.getCustomer() != null
                && appointment.getCustomer().getId().equals(principal.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to modify images for this appointment");
    }
}
