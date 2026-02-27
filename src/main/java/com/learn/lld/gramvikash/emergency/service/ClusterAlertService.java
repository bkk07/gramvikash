package com.learn.lld.gramvikash.emergency.service;

import com.learn.lld.gramvikash.emergency.enums.EmergencyType;
import com.learn.lld.gramvikash.emergency.repository.EmergencyRequestRepository;
import com.learn.lld.gramvikash.user.entity.Farmer;
import com.learn.lld.gramvikash.user.repository.FarmerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterAlertService {

    // Emergency types that trigger cluster alerts
    private static final Set<EmergencyType> ALERTABLE_TYPES = Set.of(
            EmergencyType.SNAKE_BITE,
            EmergencyType.LIVESTOCK_EMERGENCY
    );

    // If >= this many cases are found in the radius/time window, fire the alert
    private static final int CLUSTER_THRESHOLD = 3;

    // Time window (minutes) — look back this many minutes
    private static final int TIME_WINDOW_MINUTES = 60;

    // Radius to scan for cluster cases and to find farmers to notify
    private static final double CLUSTER_RADIUS_KM = 5.0;

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final FarmerRepository farmerRepository;
    private final TwilioService twilioService;

    /**
     * Called after every emergency is saved.
     * Returns a ClusterAlertResult describing whether an alert was sent.
     */
    public ClusterAlertResult checkAndAlert(EmergencyType emergencyType,
                                            double lat, double lng) {

        // Only SNAKE_BITE and LIVESTOCK_EMERGENCY trigger cluster alerts
        if (!ALERTABLE_TYPES.contains(emergencyType)) {
            return ClusterAlertResult.notTriggered();
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(TIME_WINDOW_MINUTES);

        int count = emergencyRequestRepository.countRecentSameTypeInRadius(
                lat, lng, CLUSTER_RADIUS_KM, emergencyType.name(), since);

        log.info("[ClusterAlert] {} cases of {} found within {}km in the last {} minutes",
                count, emergencyType, CLUSTER_RADIUS_KM, TIME_WINDOW_MINUTES);

        if (count < CLUSTER_THRESHOLD) {
            return ClusterAlertResult.notTriggered();
        }

        // Threshold breached — find and alert all nearby farmers
        List<Farmer> nearbyFarmers = farmerRepository.findNearbyActiveFarmers(lat, lng, CLUSTER_RADIUS_KM);

        String smsMessage = buildAlertMessage(emergencyType, count);

        int notifiedCount = 0;
        for (Farmer farmer : nearbyFarmers) {
            twilioService.sendEmergencySms(farmer.getPhoneNumber(), smsMessage, lat, lng);
            notifiedCount++;
        }

        log.warn("[ClusterAlert] ALERT SENT — {} farmers notified about {} cluster ({} cases in {}km)",
                notifiedCount, emergencyType, count, CLUSTER_RADIUS_KM);

        return ClusterAlertResult.triggered(emergencyType, count, notifiedCount, smsMessage);
    }

    private String buildAlertMessage(EmergencyType type, int count) {
        return switch (type) {
            case SNAKE_BITE -> String.format(
                    "ALERT: %d snake bites nearby. Stay alert!", count);

            case LIVESTOCK_EMERGENCY -> String.format(
                    "ALERT: %d livestock cases nearby. Isolate animals!", count);

            default -> String.format(
                    "ALERT: Multiple %s cases nearby. Stay safe!", type);
        };
    }

    // ────────────────── Result record ──────────────────

    public record ClusterAlertResult(
            boolean triggered,
            EmergencyType emergencyType,
            int caseCount,
            int farmersNotified,
            String alertMessage
    ) {
        static ClusterAlertResult notTriggered() {
            return new ClusterAlertResult(false, null, 0, 0, null);
        }

        static ClusterAlertResult triggered(EmergencyType type, int cases,
                                             int farmers, String message) {
            return new ClusterAlertResult(true, type, cases, farmers, message);
        }
    }
}
