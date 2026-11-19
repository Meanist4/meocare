package com.meocare.meocare;

import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.MedicalRecordRepository;
import com.meocare.meocare.repository.PetImageRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.RoleRepository;
import com.meocare.meocare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class MeocareApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PetRepository petRepository;

    @MockitoBean
    private PetImageRepository petImageRepository;

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private MedicalRecordRepository medicalRecordRepository;

    @Test
    void contextLoads() {
    }

}
