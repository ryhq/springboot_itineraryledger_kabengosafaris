package com.itineraryledger.kabengosafaris.Translation.Account.DTOs;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTranslationAccountDTO {

    @NotBlank(message = "Account name is required")
    private String name;

    private String description;

    /**
     * Provider type as integer: 1=LIBRE_TRANSLATE, 2=GOOGLE_CLOUD, 3=DEEPL
     */
    @NotNull(message = "Provider type is required")
    private Integer providerType;

    /**
     * API key for the translation provider. Will be encrypted before storing.
     * Required for Google Cloud and DeepL. Optional for LibreTranslate.
     */
    private String apiKey;

    /**
     * Provider endpoint URL.
     * Required for LibreTranslate (e.g., http://localhost:5000).
     * Required for Google Cloud (e.g., https://translation.googleapis.com/language/translate/v2).
     * Required for DeepL (e.g., https://api-free.deepl.com or https://api.deepl.com for paid).
     */
    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    /**
     * Request timeout in seconds. Default: 30
     */
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeoutSeconds;
}
