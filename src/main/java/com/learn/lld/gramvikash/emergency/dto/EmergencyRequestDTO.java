package com.learn.lld.gramvikash.emergency.dto;

import com.learn.lld.gramvikash.emergency.enums.EmergencySeverity;
import com.learn.lld.gramvikash.emergency.enums.EmergencyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyRequestDTO {

    @NotNull(message = "Farmer ID is required")
    private Long farmerId;

    @NotNull(message = "Emergency type is required")
    private EmergencyType emergencyType;

    @NotNull(message = "Severity is required")
    private EmergencySeverity severity;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;

    private String imageUrl;

    private String voiceNoteUrl;
}
