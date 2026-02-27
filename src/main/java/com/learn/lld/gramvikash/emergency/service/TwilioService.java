package com.learn.lld.gramvikash.emergency.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromPhoneNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("[Twilio] Initialized with account SID: {}...", accountSid.substring(0, Math.min(8, accountSid.length())));
    }

    /**
     * Sends a real SMS to the given phone number with the emergency message
     * and an OpenStreetMap link to the farmer's location.
     */
    public void sendEmergencySms(String toPhone, String emergencyMessage, double latitude, double longitude) {
        try {
            String locationUrl = "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude + "#map=17/" + latitude + "/" + longitude;

            String fullMessage = emergencyMessage + "\n" + locationUrl;
            log.info(fullMessage);

            Message message = Message.creator(
                    new PhoneNumber(toPhone),
                    new PhoneNumber(fromPhoneNumber),
                    fullMessage
            ).create();

            log.info("[Twilio] SMS sent to {} | SID: {}", toPhone, message.getSid());
        } catch (Exception e) {
            log.error("[Twilio] Failed to send SMS to {}: {}", toPhone, e.getMessage());
        }
    }

    /**
     * Backward-compatible call without coordinates (just logs).
     */
    public void makeEmergencyCall(String phone, String message) {
        log.info("[Twilio] Call (simulated) to {} with message: {}", phone, message);
    }
}
