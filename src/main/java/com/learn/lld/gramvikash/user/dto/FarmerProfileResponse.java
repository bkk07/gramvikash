package com.learn.lld.gramvikash.user.dto;

import com.learn.lld.gramvikash.user.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerProfileResponse {
    private Long id;
    private String phoneNumber;
    private String userName;
    private String fullName;
    private String gender;
    private Integer age;
    private Boolean minority;
    private Language language;
    private String districtName;
    private String mandalName;
    private Double latitude;
    private Double longitude;
    private Boolean isBpl;
    private Boolean isActive;
}
