package com.learn.lld.gramvikash.emergency.dto;

import com.learn.lld.gramvikash.emergency.enums.EmergencySeverity;
import com.learn.lld.gramvikash.emergency.enums.EmergencyType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceEmergencyResponseDTO {

    // AI transcription & classification
    private String transcript;
    private EmergencyType classifiedEmergencyType;
    private EmergencySeverity classifiedSeverity;
    private String aiReasoning;

    // The full emergency dispatch response (reuses existing DTO)
    private EmergencyResponseDTO emergencyResponse;
}
