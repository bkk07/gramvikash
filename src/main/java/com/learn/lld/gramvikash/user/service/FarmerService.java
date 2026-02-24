package com.learn.lld.gramvikash.user.service;

import com.learn.lld.gramvikash.common.config.JwtTokenProvider;
import com.learn.lld.gramvikash.user.dto.*;
import com.learn.lld.gramvikash.user.entity.District;
import com.learn.lld.gramvikash.user.entity.Farmer;
import com.learn.lld.gramvikash.user.entity.Mandal;
import com.learn.lld.gramvikash.user.repository.FarmerRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        farmer.setGender(request.getGender());
        farmer.setAge(request.getAge());
        farmer.setMinority(request.getMinority() != null ? request.getMinority() : false);
        farmer.setLanguage(request.getLanguage());
        farmer.setIsBpl(request.getIsBpl() != null ? request.getIsBpl() : false);

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

        // Save farmer
        Farmer savedFarmer = farmerRepository.save(farmer);

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

        return FarmerProfileResponse.builder()
                .id(farmer.getId())
                .phoneNumber(farmer.getPhoneNumber())
                .userName(farmer.getUserName())
                .fullName(farmer.getFullName())
                .gender(farmer.getGender())
                .age(farmer.getAge())
                .minority(farmer.getMinority())
                .language(farmer.getLanguage())
                .districtName(farmer.getDistrict().getName())
                .mandalName(farmer.getMandal().getName())
                .latitude(farmer.getLatitude())
                .longitude(farmer.getLongitude())
                .isBpl(farmer.getIsBpl())
                .isActive(farmer.getIsActive())
                .build();
    }
}
