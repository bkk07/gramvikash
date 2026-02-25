package com.learn.lld.gramvikash.diagnostic.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticRequest {
    private String userQuery;
    private Long farmerId;
    private String language;   // "en" | "hi" | "te"
    private String region;
}
