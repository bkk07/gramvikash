package com.learn.lld.gramvikash.diagnostic.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticResponse {
    private Long sessionId;
    private String classifiedDisease;
    private String classifiedCrop;
    private Double confidence;
    private String diagnosis;
    private List<String> symptomsMatched;
    private Map<String, Object> managementAdvice;
    private Boolean regionSpecific;
    private String source;
    private String language;
}
