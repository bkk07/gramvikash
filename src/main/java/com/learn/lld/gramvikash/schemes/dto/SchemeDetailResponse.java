package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeDetailResponse {
    private Long id;
    private String schemeName;
    private String schemeCode;
    private String description;
    private String benefitDetails;
    private String category;
    private String state;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<EligibilityGroupDetail> eligibilityGroups;
    private List<FaqDetail> faqs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EligibilityGroupDetail {
        private String groupName;
        private String groupOperator;
        private List<RuleDetail> rules;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RuleDetail {
        private String fieldName;
        private String operator;
        private String value;
        private String fieldType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaqDetail {
        private Long id;
        private String question;
        private String answer;
        private String language;
        private Integer displayOrder;
    }
}
