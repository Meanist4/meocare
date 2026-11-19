package com.meocare.meocare.controller;

import com.meocare.meocare.service.AppointmentImageService;
import com.meocare.meocare.service.AppointmentImageService.ImageRequest;
import com.meocare.meocare.service.AppointmentImageService.ImageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** PHẦN TUỲ CHỌN: xoá cùng AppointmentImageService nếu chưa có entity/repository ảnh lịch hẹn. */
@RestController
@RequestMapping("/api/appointments/{appointmentId}/images")
@RequiredArgsConstructor
public class AppointmentImageController {

    private final AppointmentImageService imageService;

    @PostMapping
    public ResponseEntity<ImageResponse> add(@PathVariable Long appointmentId,
                                             @Valid @RequestBody ImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageService.addImage(appointmentId, request));
    }

    @GetMapping
    public ResponseEntity<List<ImageResponse>> list(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(imageService.getImages(appointmentId));
    }
}
