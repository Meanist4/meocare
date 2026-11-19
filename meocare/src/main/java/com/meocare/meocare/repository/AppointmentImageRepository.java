package com.meocare.meocare.repository;

import com.meocare.meocare.entity.AppointmentImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentImageRepository extends JpaRepository<AppointmentImage, Long> {

    List<AppointmentImage> findByAppointment_Id(Long appointmentId);

    Optional<AppointmentImage> findByIdAndAppointment_Id(Long id, Long appointmentId);

    long countByAppointment_Id(Long appointmentId);
}
