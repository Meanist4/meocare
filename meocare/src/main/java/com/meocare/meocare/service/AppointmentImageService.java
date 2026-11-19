package com.meocare.meocare.service;

import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.AppointmentImage;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentImageRepository;
import com.meocare.meocare.repository.AppointmentRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PHẦN TUỲ CHỌN: chỉ giữ nếu dự án đã có entity AppointmentImage + AppointmentImageRepository.
 * ImageRequest/ImageResponse đặt lồng ở đây để không trùng tên với DTO sẵn có của bạn;
 * nếu đã có DTO riêng thì thay vào.
 */
@Service
@RequiredArgsConstructor
public class AppointmentImageService {

    public record ImageRequest(@NotBlank String imageUrl) {
    }

    public record ImageResponse(Long id, Long appointmentId, String imageUrl) {
    }

    private final AppointmentImageRepository imageRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public ImageResponse addImage(Long appointmentId, ImageRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lịch hẹn với id " + appointmentId));

        AppointmentImage image = new AppointmentImage();
        image.setAppointment(appointment);
        image.setImageUrl(request.imageUrl()); // GIẢ ĐỊNH: tên trường URL ảnh
        return toResponse(imageRepository.save(image), appointmentId);
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(Long appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException("Không tìm thấy lịch hẹn với id " + appointmentId);
        }
        return imageRepository.findByAppointment_Id(appointmentId).stream()
                .map(i -> toResponse(i, appointmentId))
                .toList();
    }

    private ImageResponse toResponse(AppointmentImage i, Long appointmentId) {
        return new ImageResponse(i.getId(), appointmentId, i.getImageUrl());
    }
}
