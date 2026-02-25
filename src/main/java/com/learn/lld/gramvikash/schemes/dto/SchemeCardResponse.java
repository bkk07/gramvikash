package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeCardResponse {
    private Long id;
    private String schemeName;
    private String schemeCode;
    private String category;
    private String state;           // null = Central
    private String benefitSummary;  // truncated benefitDetails
    private Boolean isActive;
}
