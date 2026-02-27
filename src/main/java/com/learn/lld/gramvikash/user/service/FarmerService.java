package com.learn.lld.gramvikash.user.service;

import com.learn.lld.gramvikash.common.config.JwtTokenProvider;
import com.learn.lld.gramvikash.user.dto.*;
import com.learn.lld.gramvikash.user.entity.District;
import com.learn.lld.gramvikash.user.entity.Farmer;
import com.learn.lld.gramvikash.user.entity.Mandal;
import com.learn.lld.gramvikash.user.entity.State;
import com.learn.lld.gramvikash.user.repository.FarmerRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FarmerService {

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    public RegisterResponse register(RegisterRequest request) {
        // Check if username already exists
        if (farmerRepository.existsByUserName(request.getUserName())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if phone number already exists
        if (farmerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already exists");
        }

        // Create new farmer
        Farmer farmer = new Farmer();
        farmer.setPhoneNumber(request.getPhoneNumber());
        farmer.setUserName(request.getUserName());
        farmer.setPassword(passwordEncoder.encode(request.getPassword()));
        farmer.setFullName(request.getFullName());
        farmer.setDob(request.getDob());
        farmer.setLanguage(request.getLanguage());

        // Fetch and set state
        State state = entityManager.find(State.class, request.getStateId());
        if (state == null) {
            throw new RuntimeException("State not found");
        }
        farmer.setState(state);

        // Fetch and set district and mandal
        District district = entityManager.find(District.class, request.getDistrictId());
        if (district == null) {
            throw new RuntimeException("District not found");
        }
        farmer.setDistrict(district);

        Mandal mandal = entityManager.find(Mandal.class, request.getMandalId());
        if (mandal == null) {
            throw new RuntimeException("Mandal not found");
        }
        farmer.setMandal(mandal);
        farmer.setLatitude(request.getLatitude());
        farmer.setLongitude(request.getLongitude());

        // Save farmer
        Farmer savedFarmer = farmerRepository.save(farmer);

        // Log stored coordinates for debugging
        org.slf4j.LoggerFactory.getLogger(FarmerService.class)
            .info("[FarmerService] Saved farmer id={} latitude={} longitude={}",
                savedFarmer.getId(), savedFarmer.getLatitude(), savedFarmer.getLongitude());
        return new RegisterResponse(
                savedFarmer.getId(),
                savedFarmer.getUserName(),
                "Registration successful"
        );
    }

    public LoginResponse login(LoginRequest request) {
        // Find farmer by username
        Farmer farmer = farmerRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), farmer.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(farmer.getUserName());

        return new LoginResponse(
                token,
                farmer.getUserName(),
                "Login successful"
        );
    }

    public String changePassword(ChangePasswordRequest request) {
        // Find farmer by username
        Farmer farmer = farmerRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), farmer.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // Check if new password is same as old password
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("New password cannot be same as old password");
        }

        // Update password
        farmer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        farmerRepository.save(farmer);

        return "Password changed successfully";
    }

    public FarmerProfileResponse getUserProfile(String userName) {
        Farmer farmer = farmerRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer age = farmer.getDob() != null
                ? Period.between(farmer.getDob(), LocalDate.now()).getYears()
                : null;

        return FarmerProfileResponse.builder()
                .id(farmer.getId())
                .phoneNumber(farmer.getPhoneNumber())
                .userName(farmer.getUserName())
                .fullName(farmer.getFullName())
                .dob(farmer.getDob())
                .age(age)
                .language(farmer.getLanguage())
                .stateName(farmer.getState().getName())
                .districtName(farmer.getDistrict().getName())
                .mandalName(farmer.getMandal().getName())
                .latitude(farmer.getLatitude())
                .longitude(farmer.getLongitude())
                .isActive(farmer.getIsActive())
                .build();
    }

            public List<com.learn.lld.gramvikash.user.dto.FarmerProfileResponse> findNearbyFarmers(double latitude, double longitude, double radiusKm) {
            return farmerRepository.findNearbyActiveFarmers(latitude, longitude, radiusKm)
                .stream()
                .map(farmer -> com.learn.lld.gramvikash.user.dto.FarmerProfileResponse.builder()
                    .id(farmer.getId())
                    .phoneNumber(farmer.getPhoneNumber())
                    .userName(farmer.getUserName())
                    .fullName(farmer.getFullName())
                    .dob(farmer.getDob())
                    .age(farmer.getDob() != null ? Period.between(farmer.getDob(), LocalDate.now()).getYears() : null)
                    .language(farmer.getLanguage())
                    .stateName(farmer.getState() != null ? farmer.getState().getName() : null)
                    .districtName(farmer.getDistrict() != null ? farmer.getDistrict().getName() : null)
                    .mandalName(farmer.getMandal() != null ? farmer.getMandal().getName() : null)
                    .latitude(farmer.getLatitude())
                    .longitude(farmer.getLongitude())
                    .isActive(farmer.getIsActive())
                    .build())
                .collect(Collectors.toList());
            }

    public void updateLocation(Long farmerId, Double latitude, Double longitude) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found with id: " + farmerId));
        farmer.setLatitude(latitude);
        farmer.setLongitude(longitude);
        farmerRepository.save(farmer);
    }
}
