package com.itineraryledger.kabengosafaris.Translation.Providers;

import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;

import java.util.List;
import java.util.Map;

/**
 * Common interface for all translation providers (LibreTranslate, Google Cloud, DeepL).
 * Each provider implementation handles its own API format and language code normalization.
 */
public interface TranslationProvider {

    /**
     * Translate text from source to target language.
     */
    String translate(String text, String sourceLanguage, String targetLanguage) throws TranslationProviderException;

    /**
     * Check if the translation service is reachable and responding.
     */
    boolean isServiceAvailable();

    /**
     * Get available languages from the provider.
     * Returns a list of maps with at least "code" and "name" keys.
     */
    List<Map<String, String>> getAvailableLanguages() throws TranslationProviderException;

    /**
     * Detect the language of the given text.
     */
    String detectLanguage(String text) throws TranslationProviderException;

    /**
     * Check if a specific language code is supported by this provider.
     */
    boolean isLanguageSupported(String languageCode);

    /**
     * Get the provider type enum value.
     */
    TranslationProviderType getProviderType();
}
