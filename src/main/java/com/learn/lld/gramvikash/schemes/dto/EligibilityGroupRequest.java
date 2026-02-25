package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityGroupRequest {
    private String groupName;       // BASIC, LAND_RULES
    private String groupOperator;   // AND / OR
    private List<EligibilityRuleRequest> rules;
}
