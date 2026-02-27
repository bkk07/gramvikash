package com.learn.lld.gramvikash.emergency.controller;

import com.learn.lld.gramvikash.emergency.dto.AvailabilityUpdateDTO;
import com.learn.lld.gramvikash.emergency.dto.DoctorRegistrationDTO;
import com.learn.lld.gramvikash.emergency.entity.Doctor;
import com.learn.lld.gramvikash.emergency.repository.DoctorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;

    @PostMapping("/register")
    public ResponseEntity<Doctor> register(@Valid @RequestBody DoctorRegistrationDTO dto) {
        Doctor doctor = Doctor.builder()
                .name(dto.getName())
                .phone(dto.getPhone())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .available(true)
                .build();
        return ResponseEntity.ok(doctorRepository.save(doctor));
    }

    @PutMapping("/availability")
    public ResponseEntity<Doctor> updateAvailability(@Valid @RequestBody AvailabilityUpdateDTO dto) {
        Doctor doctor = doctorRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + dto.getId()));
        doctor.setAvailable(dto.getAvailable());
        return ResponseEntity.ok(doctorRepository.save(doctor));
    }
}
