package com.learn.lld.gramvikash.emergency.dto;

import com.learn.lld.gramvikash.emergency.enums.EmergencySeverity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivestockEmergencyResponseDTO {

    // Farmer's original text description
    private String farmerDescription;

    // AI vision analysis
    private EmergencySeverity classifiedSeverity;
    private String aiDiagnosis;
    private String aiReasoning;
    private String immediateAdvice;

    // The full emergency dispatch response (reuses existing DTO)
    private EmergencyResponseDTO emergencyResponse;
}
