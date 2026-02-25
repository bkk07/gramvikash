package com.learn.lld.gramvikash.diagnostic.entity;

import com.learn.lld.gramvikash.user.entity.Farmer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "diagnostic_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @Column(columnDefinition = "TEXT")
    private String userQuery;

    private String classifiedCrop;
    private String classifiedDisease;
    private Double classificationConfidence;

    @Column(columnDefinition = "TEXT")
    private String diagnosisResponse;

    private String source;       // "rag" | "llm"
    private String sourceType;   // "WEB" | "IVRS"
    private String language;
    private String region;
    private Boolean regionSpecific;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
