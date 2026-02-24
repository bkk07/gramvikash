package com.learn.lld.gramvikash.user.controller;

import com.learn.lld.gramvikash.common.config.JwtTokenProvider;
import com.learn.lld.gramvikash.common.exception.ApiResponse;
import com.learn.lld.gramvikash.user.dto.*;
import com.learn.lld.gramvikash.user.service.FarmerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farmers")
public class FarmerController {

    @Autowired
    private FarmerService farmerService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = farmerService.register(request);
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.CREATED.value(), "User registered successfully", response),
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Registration failed", e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = farmerService.login(request);
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.OK.value(), "Login successful", response),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.UNAUTHORIZED.value(), "Login failed", e.getMessage()),
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            String response = farmerService.changePassword(request);
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.OK.value(), "Password changed successfully", response),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Password change failed", e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/profile/{userName}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getUserProfile(@PathVariable String userName) {
        try {
            FarmerProfileResponse response = farmerService.getUserProfile(userName);
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.OK.value(), "User profile retrieved successfully", response),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.NOT_FOUND.value(), "User not found", e.getMessage()),
                    HttpStatus.NOT_FOUND
            );
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            boolean isValid = jwtTokenProvider.validateToken(jwtToken);
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.OK.value(), "Token validation result", isValid),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Token validation failed", e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
