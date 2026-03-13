package com.itineraryledger.kabengosafaris.Translation.Account.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TranslationAccountDTO {

    /** Obfuscated ID */
    private String id;

    private String name;
    private String description;
    private TranslationProviderType providerType;
    private String baseUrl;
    private Boolean enabled;
    private Boolean isDefault;
    private Integer timeoutSeconds;

    // Test info
    private LocalDateTime lastTestedAt;
    private String lastErrorMessage;

    // Statistics
    private Long charactersTranslated;
    private Long requestsMade;
    private Long requestsFailed;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
