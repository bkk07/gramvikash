package com.learn.lld.gramvikash.emergency.service;

import com.learn.lld.gramvikash.emergency.dto.EmergencyRequestDTO;
import com.learn.lld.gramvikash.emergency.dto.EmergencyResponseDTO;
import com.learn.lld.gramvikash.emergency.entity.Doctor;
import com.learn.lld.gramvikash.emergency.entity.Driver;
import com.learn.lld.gramvikash.emergency.entity.EmergencyRequest;
import com.learn.lld.gramvikash.emergency.enums.EmergencyStatus;
import com.learn.lld.gramvikash.emergency.repository.DoctorRepository;
import com.learn.lld.gramvikash.emergency.repository.DriverRepository;
import com.learn.lld.gramvikash.emergency.repository.EmergencyRequestRepository;
import com.learn.lld.gramvikash.emergency.service.ClusterAlertService.ClusterAlertResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyService {

    private static final double DEFAULT_RADIUS_KM = 5.0;

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final DoctorRepository doctorRepository;
    private final DriverRepository driverRepository;
    private final TwilioService twilioService;
    private final ClusterAlertService clusterAlertService;

    public EmergencyResponseDTO handleEmergency(EmergencyRequestDTO dto) {
        // 1. Persist the emergency request
        EmergencyRequest request = EmergencyRequest.builder()
                .farmerId(dto.getFarmerId())
                .emergencyType(dto.getEmergencyType())
                .severity(dto.getSeverity())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .imageUrl(dto.getImageUrl())
                .voiceNoteUrl(dto.getVoiceNoteUrl())
                .build();
        request = emergencyRequestRepository.save(request);

        // 2. Dispatch based on emergency type
        List<String> notifiedDoctors = new ArrayList<>();
        List<String> notifiedDrivers = new ArrayList<>();
        List<String> dosAndDonts;
        String message;

        switch (dto.getEmergencyType()) {
            case SNAKE_BITE -> {
                dosAndDonts = getSnakeBiteDosAndDonts();
                message = "SNAKE BITE! Rush to farmer.";
            }
            case PESTICIDE_POISONING -> {
                dosAndDonts = getPesticidePoisoningDosAndDonts();
                message = "POISONING! Farmer needs help.";
            }
            case FIRE_ACCIDENT -> {
                dosAndDonts = getFireAccidentDosAndDonts();
                message = "FIRE! Vehicle needed now.";
            }
            case TRACTOR_ACCIDENT -> {
                dosAndDonts = getTractorAccidentDosAndDonts();
                message = "ACCIDENT! Rescue needed.";
            }
            case LIVESTOCK_EMERGENCY -> {
                dosAndDonts = getLivestockEmergencyDosAndDonts();
                message = "LIVESTOCK EMERGENCY! Vet needed.";
            }
            default -> {
                dosAndDonts = List.of("Stay calm.", "Call local emergency services.");
                message = "EMERGENCY! Help needed.";
            }
        }

        // ── Notify ALL nearby drivers (any vehicle type) ──
        List<Driver> nearbyDrivers = driverRepository.findNearbyAvailableDrivers(
                dto.getLatitude(), dto.getLongitude(), DEFAULT_RADIUS_KM);
        for (Driver d : nearbyDrivers) {
            twilioService.sendEmergencySms(d.getPhone(), message, dto.getLatitude(), dto.getLongitude());
            notifiedDrivers.add(d.getName() + " (" + d.getPhone() + ")");
        }

        // ── Notify nearest doctor ──
        Optional<Doctor> nearestDoctor = doctorRepository.findNearestAvailableDoctor(
                dto.getLatitude(), dto.getLongitude());
        nearestDoctor.ifPresent(doc -> {
            twilioService.sendEmergencySms(doc.getPhone(), message, dto.getLatitude(), dto.getLongitude());
            notifiedDoctors.add(doc.getName() + " (" + doc.getPhone() + ")");
        });

        // 3. Update request status
        request.setStatus(EmergencyStatus.NOTIFIED);
        emergencyRequestRepository.save(request);

        // 4. Cluster alert check — runs after save so this request is included in count
        ClusterAlertResult clusterResult = clusterAlertService.checkAndAlert(
                dto.getEmergencyType(), dto.getLatitude(), dto.getLongitude());

        // 5. Build response
        return EmergencyResponseDTO.builder()
                .emergencyId(request.getId())
                .emergencyType(request.getEmergencyType())
                .severity(request.getSeverity())
                .status(request.getStatus())
                .timestamp(request.getTimestamp())
                .dosAndDonts(dosAndDonts)
                .notifiedDoctors(notifiedDoctors)
                .notifiedDrivers(notifiedDrivers)
                .message(notifiedDoctors.isEmpty() && notifiedDrivers.isEmpty()
                        ? "No nearby responders found. Please contact local authorities."
                        : "Emergency responders have been notified.")
                .clusterAlertTriggered(clusterResult.triggered())
                .clusterCaseCount(clusterResult.caseCount())
                .clusterFarmersNotified(clusterResult.farmersNotified())
                .clusterAlertMessage(clusterResult.alertMessage())
                .build();
    }

    // ──────────────────────────────── Static Do's and Don'ts ────────────────────────────────

    private List<String> getSnakeBiteDosAndDonts() {
        return List.of(
                "DO: Keep the victim calm and still.",
                "DO: Immobilize the bitten limb and keep it below heart level.",
                "DO: Remove rings, watches, or tight clothing near the bite.",
                "DO: Rush to the nearest hospital immediately.",
                "DON'T: Do NOT cut the wound or try to suck out the venom.",
                "DON'T: Do NOT apply ice or a tourniquet.",
                "DON'T: Do NOT give the victim alcohol or aspirin."
        );
    }

    private List<String> getPesticidePoisoningDosAndDonts() {
        return List.of(
                "DO: Move the person to fresh air immediately.",
                "DO: Remove contaminated clothing carefully.",
                "DO: Wash affected skin with soap and water for 15 minutes.",
                "DO: If swallowed, do NOT induce vomiting unless instructed by a doctor.",
                "DO: Bring the pesticide container/label to the hospital.",
                "DON'T: Do NOT give the person anything to eat or drink.",
                "DON'T: Do NOT leave the person unattended."
        );
    }

    private List<String> getFireAccidentDosAndDonts() {
        return List.of(
                "DO: Evacuate the area immediately.",
                "DO: Call the fire department (101).",
                "DO: Use water or sand to control small fires if safe.",
                "DO: Stay low to the ground if there is smoke.",
                "DON'T: Do NOT use water on electrical or oil fires.",
                "DON'T: Do NOT re-enter a burning structure.",
                "DON'T: Do NOT open doors that feel hot."
        );
    }

    private List<String> getTractorAccidentDosAndDonts() {
        return List.of(
                "DO: Turn off the engine if accessible.",
                "DO: Check for fuel leaks and move away if detected.",
                "DO: Apply first aid and stop any bleeding.",
                "DO: Do NOT move the victim if a spinal injury is suspected.",
                "DON'T: Do NOT crowd around the victim.",
                "DON'T: Do NOT attempt to move heavy machinery without equipment."
        );
    }

    private List<String> getLivestockEmergencyDosAndDonts() {
        return List.of(
                "DO: Isolate the affected animal from the rest of the herd.",
                "DO: Provide clean water and shade.",
                "DO: Note symptoms, time of onset, and recent feed changes.",
                "DO: Take a photo/video of the animal's condition for the vet.",
                "DON'T: Do NOT administer medication without vet guidance.",
                "DON'T: Do NOT handle the animal roughly.",
                "DON'T: Do NOT mix the animal back with healthy livestock."
        );
    }
}
