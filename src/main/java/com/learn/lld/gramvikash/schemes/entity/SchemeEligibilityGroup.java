package com.learn.lld.gramvikash.schemes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scheme_eligibility_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemeEligibilityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String groupName;  // BASIC, LAND_RULES

    @Column(nullable = false)
    private String groupOperator;  // AND / OR

    @ManyToOne
    @JoinColumn(name = "scheme_id", nullable = false)
    private Scheme scheme;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchemeEligibilityRule> rules = new ArrayList<>();
}
