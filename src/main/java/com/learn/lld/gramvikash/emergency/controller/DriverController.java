package com.learn.lld.gramvikash.emergency.controller;

import com.learn.lld.gramvikash.emergency.dto.AvailabilityUpdateDTO;
import com.learn.lld.gramvikash.emergency.dto.DriverRegistrationDTO;
import com.learn.lld.gramvikash.emergency.entity.Driver;
import com.learn.lld.gramvikash.emergency.repository.DriverRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverRepository driverRepository;

    @PostMapping("/register")
    public ResponseEntity<Driver> register(@Valid @RequestBody DriverRegistrationDTO dto) {
        Driver driver = Driver.builder()
                .name(dto.getName())
                .phone(dto.getPhone())
                .vehicleType(dto.getVehicleType())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .available(true)
                .build();
        return ResponseEntity.ok(driverRepository.save(driver));
    }

    @PutMapping("/availability")
    public ResponseEntity<Driver> updateAvailability(@Valid @RequestBody AvailabilityUpdateDTO dto) {
        Driver driver = driverRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Driver not found with id: " + dto.getId()));
        driver.setAvailable(dto.getAvailable());
        return ResponseEntity.ok(driverRepository.save(driver));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Driver>> getNearbyDrivers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        List<Driver> drivers = driverRepository.findNearbyAvailableDrivers(latitude, longitude, radiusKm);
        return ResponseEntity.ok(drivers);
    }
}
