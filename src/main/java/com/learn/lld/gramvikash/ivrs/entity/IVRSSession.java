package com.learn.lld.gramvikash.ivrs.entity;

import com.learn.lld.gramvikash.user.entity.Farmer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ivrs_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IVRSSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @Column(unique = true)
    private String callSid;

    private String phoneNumber;
    private String language;

    @Enumerated(EnumType.STRING)
    private CallStatus callStatus;

    @Column(columnDefinition = "TEXT")
    private String userSpeechText;

    @Column(columnDefinition = "TEXT")
    private String translatedQuery;

    @Column(columnDefinition = "TEXT")
    private String responseText;

    @Column(columnDefinition = "TEXT")
    private String translatedResponse;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }

    public enum CallStatus {
        STARTED,
        LANGUAGE_SELECTED,
        MENU_SELECTED,
        GATHERING_SYMPTOMS,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
