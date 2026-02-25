package com.learn.lld.gramvikash.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_known_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserKnownField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fieldName;   // age, landSize, income, cropType

    @Column(nullable = false)
    private String value;       // 22, 5, 150000, Paddy

    @Column(nullable = false)
    private String fieldType;   // NUMBER, STRING, BOOLEAN

    @ManyToOne
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;
}
