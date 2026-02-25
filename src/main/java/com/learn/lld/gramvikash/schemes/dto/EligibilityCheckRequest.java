package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckRequest {
    private Long farmerId;                      // auto-fetch farmer profile fields
    private String mode;                        // DISCOVER or VERIFY
    private Map<String, String> additionalFields; // extra fields provided by farmer (landSize, income, etc.)
}
