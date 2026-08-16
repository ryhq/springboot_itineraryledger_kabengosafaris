package com.itineraryledger.kabengosafaris.Translation.Account.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;

import lombok.Data;

/**
 * Everything a caller can narrow the translation-account list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card cannot
 * report a figure the table would contradict, and prev/next cannot wander out of the set on
 * screen. Spring binds it with {@code @ModelAttribute}, so every parameter the old signature
 * took is still spelled the same on the wire.
 */
@Data
public class TranslationAccountFilter {

    /** Free text across name, description and the endpoint it talks to. */
    private String keyword;

    private String name;
    private String description;
    private String baseUrl;

    /** 1=LibreTranslate, 2=Google Cloud, 3=DeepL — the wire format this module chose. */
    private Integer providerType;
    private List<Integer> providerTypes;

    private Boolean enabled;
    /** "enabled" / "disabled"; a contradictory pair cancels to no constraint. */
    private List<String> statuses;

    private Boolean isDefault;
    private Boolean hasErrors;

    /**
     * Worth checking, each of which is also a card.
     *
     * An account that last failed is one nobody noticed had failed — translations quietly
     * fall back to English when a provider is down. An account never tested is one nobody
     * has ever proved works, which is the same problem before it happens.
     */
    private List<String> qualities;

    private LocalDateTime createdAfter;

    /** The provider types asked for, however they were spelled. */
    public List<TranslationProviderType> allProviderTypes() {
        List<Integer> raw = new ArrayList<>();
        if (providerTypes != null) providerTypes.stream().filter(v -> v != null && v > 0).forEach(raw::add);
        if (providerType != null && providerType > 0 && !raw.contains(providerType)) raw.add(providerType);

        List<TranslationProviderType> out = new ArrayList<>();
        for (Integer value : raw) {
            try {
                out.add(TranslationProviderType.fromInteger(value));
            } catch (IllegalArgumentException e) {
                // an unknown provider number is a filter for nothing, not a 500
            }
        }
        return out;
    }

    public Boolean resolvedEnabled() {
        boolean yes = statuses != null && statuses.contains("enabled");
        boolean no = statuses != null && statuses.contains("disabled");
        if (yes ^ no) return yes;
        return enabled;
    }

    public boolean wants(String quality) {
        return qualities != null && qualities.contains(quality);
    }
}
