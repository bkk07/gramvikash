package com.learn.lld.gramvikash.emergency.controller;

import com.learn.lld.gramvikash.emergency.dto.EmergencyRequestDTO;
import com.learn.lld.gramvikash.emergency.dto.EmergencyResponseDTO;
import com.learn.lld.gramvikash.emergency.dto.LivestockEmergencyResponseDTO;
import com.learn.lld.gramvikash.emergency.dto.VoiceEmergencyResponseDTO;
import com.learn.lld.gramvikash.emergency.enums.EmergencyType;
import com.learn.lld.gramvikash.emergency.service.EmergencyService;
import com.learn.lld.gramvikash.emergency.service.OpenAIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
@Slf4j
public class EmergencyController {

    private final EmergencyService emergencyService;
    private final OpenAIService openAIService;

    // ──────────────── 1. Standard JSON emergency request ────────────────

    @PostMapping("/create")
    public ResponseEntity<EmergencyResponseDTO> createEmergency(
            @Valid @RequestBody EmergencyRequestDTO request) {
        EmergencyResponseDTO response = emergencyService.handleEmergency(request);
        return ResponseEntity.ok(response);
    }

    // ──────────────── 2. Voice-based emergency (multipart) ────────────────

    /**
     * Accepts a voice recording from the farmer's phone.
     * 1. OpenAI Whisper transcribes the audio
     * 2. GPT classifies emergency type + severity
     * 3. The normal dispatch flow is triggered automatically
     *
     * curl -X POST http://localhost:8080/api/emergency/voice \
     *   -F "farmerId=<uuid>" \
     *   -F "latitude=17.3850" \
     *   -F "longitude=78.4867" \
     *   -F "voiceFile=@recording.webm"
     */
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceEmergencyResponseDTO> handleVoiceEmergency(
            @RequestParam Long farmerId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestPart MultipartFile voiceFile) {

        log.info("[Voice Emergency] Received voice file from farmer {} ({}, {}), file size: {} bytes",
                farmerId, latitude, longitude, voiceFile.getSize());

        // Step 1 — Transcribe the voice note
        String transcript = openAIService.transcribeAudio(voiceFile);

        // Step 2 — Classify emergency type & severity via GPT
        OpenAIService.ClassificationResult classification = openAIService.classifyEmergency(transcript);

        log.info("[Voice Emergency] Classified as {} / {} — {}",
                classification.emergencyType(), classification.severity(), classification.reasoning());

        // Step 3 — Build the standard request DTO & dispatch
        EmergencyRequestDTO requestDTO = EmergencyRequestDTO.builder()
                .farmerId(farmerId)
                .emergencyType(classification.emergencyType())
                .severity(classification.severity())
                .latitude(latitude)
                .longitude(longitude)
                .build();

        EmergencyResponseDTO emergencyResponse = emergencyService.handleEmergency(requestDTO);

        // Step 4 — Wrap AI metadata + dispatch response
        VoiceEmergencyResponseDTO voiceResponse = VoiceEmergencyResponseDTO.builder()
                .transcript(transcript)
                .classifiedEmergencyType(classification.emergencyType())
                .classifiedSeverity(classification.severity())
                .aiReasoning(classification.reasoning())
                .emergencyResponse(emergencyResponse)
                .build();

        return ResponseEntity.ok(voiceResponse);
    }

    // ──────────────── 3. Livestock image + text emergency (multipart) ────────────────

    /**
     * Accepts a livestock photo + text description from the farmer.
     * 1. Groq Vision (llama-3.2-11b-vision-preview) analyzes the image + text
     * 2. AI classifies severity + provides diagnosis
     * 3. The normal LIVESTOCK_EMERGENCY dispatch flow is triggered
     *
     * curl -X POST http://localhost:8080/api/emergency/livestock \
     *   -F "farmerId=1" \
     *   -F "latitude=17.3850" \
     *   -F "longitude=78.4867" \
     *   -F "description=My cow is not eating since 2 days and has swelling on its leg" \
     *   -F "image=@cow_photo.jpg"
     */
    @PostMapping(value = "/livestock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LivestockEmergencyResponseDTO> handleLivestockEmergency(
            @RequestParam Long farmerId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String description,
            @RequestPart MultipartFile image) {

        log.info("[Livestock Emergency] Image from farmer {} ({}, {}), size: {} bytes, desc: {}",
                farmerId, latitude, longitude, image.getSize(), description);

        // Step 1 — Analyze image + text via Groq Vision
        OpenAIService.LivestockAnalysisResult analysis =
                openAIService.analyzeLivestockEmergency(image, description);

        log.info("[Livestock Emergency] Severity: {}, Diagnosis: {}",
                analysis.severity(), analysis.diagnosis());

        // Step 2 — Build the standard request DTO & dispatch
        EmergencyRequestDTO requestDTO = EmergencyRequestDTO.builder()
                .farmerId(farmerId)
                .emergencyType(EmergencyType.LIVESTOCK_EMERGENCY)
                .severity(analysis.severity())
                .latitude(latitude)
                .longitude(longitude)
                .build();

        EmergencyResponseDTO emergencyResponse = emergencyService.handleEmergency(requestDTO);

        // Step 3 — Wrap AI analysis + dispatch response
        LivestockEmergencyResponseDTO livestockResponse = LivestockEmergencyResponseDTO.builder()
                .farmerDescription(description)
                .classifiedSeverity(analysis.severity())
                .aiDiagnosis(analysis.diagnosis())
                .aiReasoning(analysis.reasoning())
                .immediateAdvice(analysis.immediateAdvice())
                .emergencyResponse(emergencyResponse)
                .build();

        return ResponseEntity.ok(livestockResponse);
    }
}
