package com.learn.lld.gramvikash.ivrs.service;

import com.learn.lld.gramvikash.ivrs.entity.IVRSSession;
import com.learn.lld.gramvikash.ivrs.entity.IVRSSession.CallStatus;
import com.learn.lld.gramvikash.ivrs.repository.IVRSSessionRepository;
import com.learn.lld.gramvikash.user.entity.Farmer;
import com.learn.lld.gramvikash.user.repository.FarmerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Manages the Twilio IVRS call flow:
 *   incoming → welcome → menu → gather symptoms (speech) → process → respond
 *
 * Each step returns TwiML (XML) that Twilio interprets to continue the call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IVRSService {

    private final IVRSSessionRepository sessionRepository;
    private final FarmerRepository farmerRepository;
    private final RestTemplate restTemplate;

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    // ── voice / language maps ───────────────────────────────────────────

    private static final Map<String, String> LANG_VOICE = Map.of(
            "en", "Polly.Raveena",
            "hi", "Polly.Aditi",
            "te", "Polly.Aditi"
    );

    private static final Map<String, String> LANG_CODE = Map.of(
            "en", "en-IN",
            "hi", "hi-IN",
            "te", "te-IN"
    );

    // ── multilingual messages ───────────────────────────────────────────

    private static final Map<String, String> WELCOME = Map.of(
            "en", "Welcome to Gram Vikash. Your trusted agricultural assistant.",
            "hi", "ग्राम विकास में आपका स्वागत है। आपका विश्वसनीय कृषि सहायक।",
            "te", "గ్రామ వికాశ్‌కు స్వాగతం. మీ నమ్మకమైన వ్యవసాయ సహాయకుడు."
    );

    private static final Map<String, String> MENU = Map.of(
            "en", "Press 1 for crop disease diagnosis. Press 2 for government schemes information.",
            "hi", "फसल रोग निदान के लिए 1 दबाएं। सरकारी योजनाओं की जानकारी के लिए 2 दबाएं।",
            "te", "పంట వ్యాధి నిర్ధారణ కోసం 1 నొక్కండి. ప్రభుత్వ పథకాల సమాచారం కోసం 2 నొక్కండి."
    );

    private static final Map<String, String> SYMPTOM_PROMPT = Map.of(
            "en", "Please describe your crop symptoms after the beep. Speak clearly.",
            "hi", "कृपया बीप के बाद अपनी फसल के लक्षण बताएं। स्पष्ट रूप से बोलें।",
            "te", "దయచేసి బీప్ తర్వాత మీ పంట లక్షణాలను వివరించండి. స్పష్టంగా మాట్లాడండి."
    );

    private static final Map<String, String> GOODBYE = Map.of(
            "en", "Thank you for using Gram Vikash. Goodbye!",
            "hi", "ग्राम विकास का उपयोग करने के लिए धन्यवाद। अलविदा!",
            "te", "గ్రామ వికాశ్ ఉపయోగించినందుకు ధన్యవాదాలు. వీడ్కోలు!"
    );

    private static final Map<String, String> SCHEME_COMING_SOON = Map.of(
            "en", "Scheme information service will be available soon. Thank you.",
            "hi", "योजना जानकारी सेवा जल्द ही उपलब्ध होगी। धन्यवाद।",
            "te", "పథక సమాచార సేవ త్వరలో అందుబాటులో ఉంటుంది. ధన్యవాదాలు."
    );

    // ====================================================================
    // CALL-FLOW HANDLERS
    // ====================================================================

    /**
     * 1. Incoming call – look up farmer, pick language, create session, play welcome + menu.
     */
    public String handleIncomingCall(String callSid, String fromNumber) {
        log.info("Incoming IVRS call: sid={}, from={}", callSid, fromNumber);
        String phone = normalisePhone(fromNumber);

        Farmer farmer = farmerRepository.findByPhoneNumber(phone).orElse(null);
        String lang = "en";
        if (farmer != null && farmer.getLanguage() != null) {
            lang = switch (farmer.getLanguage()) {
                case HINDI -> "hi";
                case TELUGU -> "te";
                default -> "en";
            };
        }

        IVRSSession session = IVRSSession.builder()
                .callSid(callSid)
                .phoneNumber(phone)
                .farmer(farmer)
                .language(lang)
                .callStatus(CallStatus.STARTED)
                .build();
        sessionRepository.save(session);

        return twimlWelcome(lang, callSid);
    }

    /**
     * 2. Menu digit pressed.
     */
    public String handleMenuSelection(String callSid, String digits) {
        log.info("Menu selection: digits={} for sid={}", digits, callSid);

        IVRSSession session = sessionRepository.findByCallSid(callSid)
                .orElseThrow(() -> new RuntimeException("Session not found: " + callSid));
        session.setCallStatus(CallStatus.MENU_SELECTED);
        sessionRepository.save(session);

        String lang = session.getLanguage();

        return switch (digits) {
            case "1" -> twimlGatherSymptoms(lang, callSid);
            case "2" -> twimlSay(msg(SCHEME_COMING_SOON, lang), lang, true);
            default  -> twimlMenu(lang, callSid);
        };
    }

    /**
     * 3. Speech result received – call Python IVRS pipeline, return diagnosis as voice.
     */
    public String handleSymptomsGathered(String callSid, String speechResult) {
        log.info("Symptoms for sid={}: {}", callSid, speechResult);

        IVRSSession session = sessionRepository.findByCallSid(callSid)
                .orElseThrow(() -> new RuntimeException("Session not found: " + callSid));
        session.setUserSpeechText(speechResult);
        session.setCallStatus(CallStatus.PROCESSING);
        sessionRepository.save(session);

        String lang = session.getLanguage();
        String region = null;
        if (session.getFarmer() != null && session.getFarmer().getState() != null) {
            region = session.getFarmer().getState().getName();
        }

        // ── call Python IVRS endpoint ───────────────────────────────────
        Map<String, Object> result = callPythonIVRS(speechResult, lang, region);

        String translatedResponse = (String) result.getOrDefault("translated_response", "");
        String diagnosis = (String) result.getOrDefault("diagnosis", "");
        String translatedQuery = (String) result.getOrDefault("translated_query", speechResult);

        session.setTranslatedQuery(translatedQuery);
        session.setResponseText(diagnosis);
        session.setTranslatedResponse(translatedResponse);
        session.setCallStatus(CallStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        String spoken = !translatedResponse.isEmpty() ? translatedResponse : diagnosis;
        return twimlSay(spoken + " " + msg(GOODBYE, lang), lang, true);
    }

    // ====================================================================
    // TwiML GENERATORS
    // ====================================================================

    private String twimlWelcome(String lang, String callSid) {
        String v = voice(lang), lc = langCode(lang);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="%s" language="%s">%s</Say>
                    <Gather numDigits="1" action="/api/ivrs/menu?callSid=%s" method="POST" timeout="10">
                        <Say voice="%s" language="%s">%s</Say>
                    </Gather>
                    <Say voice="%s" language="%s">%s</Say>
                </Response>
                """.formatted(
                v, lc, msg(WELCOME, lang),
                callSid,
                v, lc, msg(MENU, lang),
                v, lc, msg(GOODBYE, lang)
        );
    }

    private String twimlMenu(String lang, String callSid) {
        String v = voice(lang), lc = langCode(lang);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Gather numDigits="1" action="/api/ivrs/menu?callSid=%s" method="POST" timeout="10">
                        <Say voice="%s" language="%s">%s</Say>
                    </Gather>
                </Response>
                """.formatted(callSid, v, lc, msg(MENU, lang));
    }

    private String twimlGatherSymptoms(String lang, String callSid) {
        String v = voice(lang), lc = langCode(lang);
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Say voice="%s" language="%s">%s</Say>
                    <Gather input="speech" action="/api/ivrs/process-symptoms?callSid=%s" method="POST"
                            language="%s" speechTimeout="5" timeout="15">
                    </Gather>
                    <Say voice="%s" language="%s">%s</Say>
                </Response>
                """.formatted(
                v, lc, msg(SYMPTOM_PROMPT, lang),
                callSid,
                lc,
                v, lc, msg(GOODBYE, lang)
        );
    }

    private String twimlSay(String message, String lang, boolean hangup) {
        String v = voice(lang), lc = langCode(lang);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Response>\n");
        sb.append("    <Say voice=\"").append(v)
          .append("\" language=\"").append(lc).append("\">")
          .append(escapeXml(message))
          .append("</Say>\n");
        if (hangup) sb.append("    <Hangup/>\n");
        sb.append("</Response>");
        return sb.toString();
    }

    // ====================================================================
    // PYTHON CLIENT
    // ====================================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> callPythonIVRS(String speechText, String lang, String region) {
        String url = pythonServiceUrl + "/api/v1/ivrs/process";

        Map<String, Object> body = new HashMap<>();
        body.put("speech_text", speechText);
        body.put("language", lang);
        if (region != null) body.put("region", region);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return resp.getBody() != null ? resp.getBody() : Map.of();
        } catch (Exception e) {
            log.error("Python IVRS service call failed: {}", e.getMessage());
            Map<String, Object> fb = new HashMap<>();
            fb.put("translated_response", "Service temporarily unavailable. Please try again later.");
            fb.put("diagnosis", "Service error");
            fb.put("translated_query", speechText);
            return fb;
        }
    }

    // ====================================================================
    // UTILS
    // ====================================================================

    private String voice(String lang) {
        return LANG_VOICE.getOrDefault(lang, "Polly.Raveena");
    }

    private String langCode(String lang) {
        return LANG_CODE.getOrDefault(lang, "en-IN");
    }

    private String msg(Map<String, String> map, String lang) {
        return map.getOrDefault(lang, map.get("en"));
    }

    private String normalisePhone(String phone) {
        if (phone == null) return "";
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.startsWith("91") && phone.length() > 10) {
            phone = phone.substring(phone.length() - 10);
        }
        return phone;
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
