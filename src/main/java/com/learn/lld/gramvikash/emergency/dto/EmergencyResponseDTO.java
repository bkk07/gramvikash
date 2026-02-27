package com.learn.lld.gramvikash.emergency.dto;

import com.learn.lld.gramvikash.emergency.enums.EmergencySeverity;
import com.learn.lld.gramvikash.emergency.enums.EmergencyStatus;
import com.learn.lld.gramvikash.emergency.enums.EmergencyType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyResponseDTO {

    private UUID emergencyId;
    private EmergencyType emergencyType;
    private EmergencySeverity severity;
    private EmergencyStatus status;
    private LocalDateTime timestamp;

    private List<String> dosAndDonts;

    private List<String> notifiedDoctors;
    private List<String> notifiedDrivers;

    private String message;

    // Cluster alert fields — populated when threshold is breached
    private boolean clusterAlertTriggered;
    private int clusterCaseCount;
    private int clusterFarmersNotified;
    private String clusterAlertMessage;
}
