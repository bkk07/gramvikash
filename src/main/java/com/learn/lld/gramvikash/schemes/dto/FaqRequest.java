package com.learn.lld.gramvikash.schemes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaqRequest {
    private String question;
    private String answer;
    private String language;        // EN, HI, TE
    private Integer displayOrder;
}
