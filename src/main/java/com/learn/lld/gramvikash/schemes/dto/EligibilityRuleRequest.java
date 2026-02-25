package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRuleRequest {
    private String fieldName;   // age, landSize, income
    private String operator;    // =, >, <, >=, <=, IN
    private String value;       // 18, 2, Paddy
    private String fieldType;   // NUMBER, STRING, BOOLEAN
}
