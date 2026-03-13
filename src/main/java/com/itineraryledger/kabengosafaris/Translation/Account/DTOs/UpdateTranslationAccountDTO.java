package com.itineraryledger.kabengosafaris.Translation.Account.DTOs;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTranslationAccountDTO {

    private String name;

    private String description;

    /** Provider type: 1=LIBRE_TRANSLATE, 2=GOOGLE_CLOUD, 3=DEEPL */
    private Integer providerType;

    /** API key — will be re-encrypted if provided */
    private String apiKey;

    /** Provider endpoint URL */
    private String baseUrl;

    /** Enable/disable the account */
    private Boolean enabled;

    /** Set as default account (only one at a time) */
    private Boolean isDefault;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeoutSeconds;
}
