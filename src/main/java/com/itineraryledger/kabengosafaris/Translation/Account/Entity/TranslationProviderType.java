package com.itineraryledger.kabengosafaris.Translation.Account.Entity;

/**
 * Supported translation provider types.
 */
public enum TranslationProviderType {

    LIBRE_TRANSLATE("LibreTranslate"),
    GOOGLE_CLOUD("Google Cloud Translation"),
    DEEPL("DeepL");

    private final String displayName;

    TranslationProviderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convert integer to provider type (for API input).
     * 1=LIBRE_TRANSLATE, 2=GOOGLE_CLOUD, 3=DEEPL
     */
    public static TranslationProviderType fromInteger(Integer value) {
        if (value == null) return null;
        return switch (value) {
            case 1 -> LIBRE_TRANSLATE;
            case 2 -> GOOGLE_CLOUD;
            case 3 -> DEEPL;
            default -> throw new IllegalArgumentException("Invalid provider type: " + value + ". Valid values: 1 (LibreTranslate), 2 (Google Cloud), 3 (DeepL)");
        };
    }

    /**
     * Convert provider type to integer for API output.
     */
    public Integer toInteger() {
        return switch (this) {
            case LIBRE_TRANSLATE -> 1;
            case GOOGLE_CLOUD -> 2;
            case DEEPL -> 3;
        };
    }
}
