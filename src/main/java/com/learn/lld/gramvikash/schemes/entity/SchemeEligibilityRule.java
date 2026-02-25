package com.learn.lld.gramvikash.schemes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scheme_eligibility_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemeEligibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fieldName;   // age, landSize, income

    @Column(nullable = false)
    private String operator;    // =, >, <, >=, <=, IN

    @Column(nullable = false)
    private String value;       // 18, 2, Paddy

    @Column(nullable = false)
    private String fieldType;   // NUMBER, STRING, BOOLEAN

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private SchemeEligibilityGroup group;
}
