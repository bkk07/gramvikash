package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchemeRequest {
    private String schemeName;
    private String schemeCode;
    private String description;
    private String benefitDetails;
    private String category;
    private String state; // null for Central schemes
    private List<EligibilityGroupRequest> eligibilityGroups;
    private List<FaqRequest> faqs;
}
