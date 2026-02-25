package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityResultResponse {
    private String mode;  // DISCOVER or VERIFY
    private int totalSchemesEvaluated;
    private List<MatchedScheme> eligibleSchemes;
    private List<MatchedScheme> almostEligibleSchemes;
    private List<MatchedScheme> ineligibleSchemes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchedScheme {
        private Long schemeId;
        private String schemeName;
        private String schemeCode;
        private String category;
        private String benefitSummary;
        private boolean eligible;
        private int failedRuleCount;
        private int totalRules;
        private String reasonMessage;       // Natural language reason string
        private List<String> missingFields; // Fields not provided (discover mode)
    }
}
