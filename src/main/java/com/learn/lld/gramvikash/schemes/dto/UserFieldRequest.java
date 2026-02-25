package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFieldRequest {
    private Long farmerId;
    private List<FieldEntry> fields;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldEntry {
        private String fieldName;   // landSize, income, cropType
        private String value;       // 5, 150000, Paddy
        private String fieldType;   // NUMBER, STRING, BOOLEAN
    }
}
