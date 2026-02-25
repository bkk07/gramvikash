package com.learn.lld.gramvikash.ivrs.controller;

import com.learn.lld.gramvikash.ivrs.service.IVRSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Twilio webhook controller for IVRS.
 *
 * <pre>
 * Call flow:
 *   /incoming         → welcome + menu
 *   /menu             → route by digit
 *   /process-symptoms → speech → Python RAG → voice response
 * </pre>
 *
 * All responses are TwiML (XML) consumed directly by Twilio.
 */
@RestController
@RequestMapping("/api/ivrs")
@RequiredArgsConstructor
@Slf4j
public class IVRSController {

    private final IVRSService ivrsService;

    /**
     * Twilio incoming-call webhook.
     */
    @PostMapping(value = "/incoming", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleIncomingCall(
            @RequestParam("CallSid") String callSid,
            @RequestParam("From") String from
    ) {
        log.info("IVRS incoming: CallSid={}, From={}", callSid, from);
        return ResponseEntity.ok(ivrsService.handleIncomingCall(callSid, from));
    }

    /**
     * Menu-selection webhook – receives the digit the user pressed.
     */
    @PostMapping(value = "/menu", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleMenuSelection(
            @RequestParam("callSid") String callSid,
            @RequestParam("Digits") String digits
    ) {
        log.info("IVRS menu: CallSid={}, Digits={}", callSid, digits);
        return ResponseEntity.ok(ivrsService.handleMenuSelection(callSid, digits));
    }

    /**
     * Symptom-processing webhook – receives transcribed speech.
     */
    @PostMapping(value = "/process-symptoms", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleSymptoms(
            @RequestParam("callSid") String callSid,
            @RequestParam("SpeechResult") String speechResult
    ) {
        log.info("IVRS symptoms: CallSid={}, Speech={}", callSid, speechResult);
        return ResponseEntity.ok(ivrsService.handleSymptomsGathered(callSid, speechResult));
    }
}
