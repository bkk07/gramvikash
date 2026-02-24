package com.learn.lld.gramvikash.user.dto;

import com.learn.lld.gramvikash.user.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String phoneNumber;
    private String userName;
    private String password;
    private String fullName;
    private String gender;
    private Integer age;
    private Boolean minority;
    private Language language;
    private Long districtId;
    private Long mandalId;
    private Boolean isBpl;
}
