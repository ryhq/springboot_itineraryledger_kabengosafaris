package com.itineraryledger.kabengosafaris.Translation.Account.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranslationProviderType providerType;

    /** Encrypted API key (via EncryptionUtil) */
    @Column(length = 500)
    private String apiKey;

    /** Provider endpoint URL. Required for LibreTranslate, optional with defaults for Google/DeepL. */
    private String baseUrl;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Boolean isDefault;

    @Column(nullable = false)
    private Integer timeoutSeconds;

    private LocalDateTime lastTestedAt;

    @Column(length = 1000)
    private String lastErrorMessage;

    // --- Statistics ---

    @Column(nullable = false)
    private Long charactersTranslated;

    @Column(nullable = false)
    private Long requestsMade;

    @Column(nullable = false)
    private Long requestsFailed;

    // --- Audit ---

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (enabled == null) enabled = false;
        if (isDefault == null) isDefault = false;
        if (timeoutSeconds == null) timeoutSeconds = 30;
        if (charactersTranslated == null) charactersTranslated = 0L;
        if (requestsMade == null) requestsMade = 0L;
        if (requestsFailed == null) requestsFailed = 0L;
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
