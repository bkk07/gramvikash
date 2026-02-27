package com.learn.lld.gramvikash.emergency.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.lld.gramvikash.emergency.enums.EmergencySeverity;
import com.learn.lld.gramvikash.emergency.enums.EmergencyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Uses Groq's OpenAI-compatible API:
 *  - Whisper large-v3-turbo        → voice transcription
 *  - llama-3.1-8b-instant          → voice emergency classification
 *  - llama-3.2-11b-vision-preview  → livestock image + text analysis
 *
 * Free tier, no quota issues, extremely fast.
 */
@Service
@Slf4j
public class OpenAIService {

    private static final String GROQ_WHISPER_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String GROQ_CHAT_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groq.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // ─────────────────── 1. Transcribe voice via Groq Whisper ───────────────────

    public String transcribeAudio(MultipartFile audioFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("model", "whisper-large-v3-turbo");
            body.add("response_format", "json");
            body.add("file", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    String original = audioFile.getOriginalFilename();
                    return (original != null && !original.isBlank()) ? original : "voice.webm";
                }
            });

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    GROQ_WHISPER_URL, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String transcript = root.path("text").asText("");
            log.info("[Groq Whisper] Transcript: {}", transcript);
            return transcript;
        } catch (IOException e) {
            log.error("[Groq Whisper] Failed to transcribe audio", e);
            throw new RuntimeException("Failed to transcribe voice note: " + e.getMessage());
        }
    }

    // ─────────────────── 2. Classify emergency via Groq LLaMA ───────────────────

    public ClassificationResult classifyEmergency(String transcript) {
        try {
            String systemPrompt = """
                    You are a rural emergency triage assistant in India.
                    Given a farmer's voice description of an emergency, classify it into exactly:

                    emergencyType: one of SNAKE_BITE, PESTICIDE_POISONING, FIRE_ACCIDENT, TRACTOR_ACCIDENT
                    severity: one of LOW, MEDIUM, HIGH, CRITICAL

                    Note: Do NOT classify as LIVESTOCK_EMERGENCY — livestock cases are handled separately via image upload.

                    Severity rules:
                    - CRITICAL: life-threatening, unconscious, heavy bleeding, difficulty breathing, multiple victims
                    - HIGH: serious injury, significant pain, spreading fire, animal in severe distress
                    - MEDIUM: moderate symptoms, manageable pain, contained situation
                    - LOW: minor symptoms, no immediate danger, precautionary

                    Respond ONLY with valid JSON (no markdown, no explanation):
                    {"emergencyType": "...", "severity": "...", "reasoning": "one line reason"}
                    """;

            Map<String, Object> requestBody = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", "Farmer says: \"" + transcript + "\"")
                    ),
                    "temperature", 0.1,
                    "max_tokens", 200
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    GROQ_CHAT_URL, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0)
                    .path("message").path("content").asText("");
            log.info("[Groq LLaMA] Classification response: {}", content);

            String jsonStr = extractJson(content);
            JsonNode classification = objectMapper.readTree(jsonStr);
            EmergencyType type = EmergencyType.valueOf(
                    classification.path("emergencyType").asText());
            EmergencySeverity severity = EmergencySeverity.valueOf(
                    classification.path("severity").asText());
            String reasoning = classification.path("reasoning").asText("");

            return new ClassificationResult(type, severity, reasoning);
        } catch (Exception e) {
            log.error("[Groq LLaMA] Classification failed, defaulting to SNAKE_BITE/HIGH", e);
            return new ClassificationResult(
                    EmergencyType.SNAKE_BITE,
                    EmergencySeverity.HIGH,
                    "Classification failed — defaulting to safe high-severity response"
            );
        }
    }

    // ─────────────────── 3. Livestock image + text analysis via Groq Vision ───────────────────

    /**
     * Sends the livestock image (base64) + farmer's text description to
     * Groq's vision model. Returns severity classification + AI diagnosis.
     *
     * Uses llama-3.2-90b-vision-preview with image as a base64 data URI.
     * Falls back to text-only analysis with llama-3.3-70b-versatile if vision fails.
     */
    public LivestockAnalysisResult analyzeLivestockEmergency(MultipartFile image, String description) {
        // First try vision model with image
        try {
            return analyzeWithVision(image, description);
        } catch (Exception visionEx) {
            log.warn("[Groq Vision] Vision model failed: {}. Falling back to text-only analysis.",
                    visionEx.getMessage());
        }

        // Fallback: text-only analysis using the powerful 70b model
        try {
            return analyzeTextOnly(description);
        } catch (Exception textEx) {
            log.error("[Groq Text] Text-only livestock analysis also failed", textEx);
            return new LivestockAnalysisResult(
                    EmergencySeverity.HIGH,
                    "Analysis failed — visual inspection needed",
                    "Error: " + textEx.getMessage(),
                    "Keep the animal calm, isolate from herd, and wait for the veterinarian."
            );
        }
    }

    private LivestockAnalysisResult analyzeWithVision(MultipartFile image, String description) throws Exception {
        String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

        String instructions = """
                You are a veterinary emergency triage assistant for rural India.
                Analyze this livestock image and the farmer's description to determine:
                1. severity: one of LOW, MEDIUM, HIGH, CRITICAL
                2. diagnosis: What is likely wrong with the animal (one line)
                3. reasoning: Why you chose this severity (one line)
                4. immediateAdvice: What the farmer should do RIGHT NOW (2-3 actionable steps)

                Severity rules:
                - CRITICAL: animal dying, severe bleeding, unable to stand, contagious disease suspected
                - HIGH: visible wounds, broken limbs, high fever signs, not eating for days
                - MEDIUM: mild injury, skin disease, limping, reduced appetite
                - LOW: minor symptoms, routine concern, preventive check

                Respond ONLY with valid JSON: {"severity":"...","diagnosis":"...","reasoning":"...","immediateAdvice":"..."}
                
                Farmer describes: "%s"
                """.formatted(description);

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", instructions),
                        Map.of("type", "image_url",
                                "image_url", Map.of(
                                        "url", "data:" + mimeType + ";base64," + base64Image
                                ))
                )
        );

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.2-90b-vision-preview",
                "messages", List.of(userMessage),
                "temperature", 0.1,
                "max_tokens", 400
        );

        return callGroqAndParse(requestBody, "Vision");
    }

    private LivestockAnalysisResult analyzeTextOnly(String description) throws Exception {
        String systemPrompt = """
                You are a veterinary emergency triage assistant for rural India.
                A farmer has described their livestock's condition. Based on the description,
                determine:
                1. severity: one of LOW, MEDIUM, HIGH, CRITICAL
                2. diagnosis: What is likely wrong with the animal (one line)
                3. reasoning: Why you chose this severity (one line)
                4. immediateAdvice: What the farmer should do RIGHT NOW (2-3 actionable steps)

                Severity rules:
                - CRITICAL: animal dying, severe bleeding, unable to stand, contagious disease suspected
                - HIGH: visible wounds, broken limbs, high fever signs, not eating for days
                - MEDIUM: mild injury, skin disease, limping, reduced appetite
                - LOW: minor symptoms, routine concern, preventive check

                Respond ONLY with valid JSON: {"severity":"...","diagnosis":"...","reasoning":"...","immediateAdvice":"..."}
                """;

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content",
                                "Farmer describes their livestock problem: \"" + description + "\"")
                ),
                "temperature", 0.1,
                "max_tokens", 400
        );

        return callGroqAndParse(requestBody, "Text-Only");
    }

    private LivestockAnalysisResult callGroqAndParse(Map<String, Object> requestBody, String tag) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(
                objectMapper.writeValueAsString(requestBody), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                GROQ_CHAT_URL, HttpMethod.POST, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").get(0)
                .path("message").path("content").asText("");
        log.info("[Groq {}] Livestock analysis response: {}", tag, content);

        String jsonStr = extractJson(content);
        JsonNode analysis = objectMapper.readTree(jsonStr);

        EmergencySeverity severity = EmergencySeverity.valueOf(
                analysis.path("severity").asText("HIGH"));
        String diagnosis = analysis.path("diagnosis").asText("Unable to determine");
        String reasoning = analysis.path("reasoning").asText("");
        String immediateAdvice = analysis.path("immediateAdvice").asText("");

        return new LivestockAnalysisResult(severity, diagnosis, reasoning, immediateAdvice);
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    public record ClassificationResult(
            EmergencyType emergencyType,
            EmergencySeverity severity,
            String reasoning
    ) {}

    public record LivestockAnalysisResult(
            EmergencySeverity severity,
            String diagnosis,
            String reasoning,
            String immediateAdvice
    ) {}
}
