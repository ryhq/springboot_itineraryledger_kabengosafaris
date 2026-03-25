package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProvider;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProviderFactory;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translates @Translatable String fields on any DTO using the existing
 * TranslationService (LibreTranslate + cache).
 * Recurses into nested DTOs and List&lt;DTO&gt; fields automatically.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublicTranslationService {

    private final TranslationService translationService;
    private final TranslationSettingGetterServices settingsService;
    private final TranslationProviderFactory providerFactory;

    /** Cache reflected translatable fields per class to avoid repeated reflection. */
    private final Map<Class<?>, List<Field>> translatableFieldsCache = new ConcurrentHashMap<>();

    /**
     * Parse an Accept-Language header value into a clean 2-char language code.
     * Examples: "fr-FR,fr;q=0.9,en;q=0.8" → "fr", "sw" → "sw", null → "en"
     */
    public String parseLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) return "en";
        // Take the first language tag (highest priority)
        String primary = acceptLanguage.split(",")[0].split(";")[0].trim();
        // Extract just the language code (before any region subtag)
        String langCode = primary.split("-")[0].toLowerCase();
        if (langCode.length() < 2) return "en";
        langCode = langCode.substring(0, 2);
        // Validate against supported languages
        if (!settingsService.isLanguageSupported(langCode)) return "en";
        return langCode;
    }

    /**
     * Translate all @Translatable String fields on a DTO.
     * If targetLang is "en", null, or blank, this is a no-op.
     * On per-field failure, the original value is preserved.
     */
    public <T> T translateDto(T dto, String targetLang) {
        if (dto == null || targetLang == null || targetLang.isBlank() || "en".equalsIgnoreCase(targetLang)) {
            return dto;
        }
        if (!translationService.isAvailable()) {
            log.debug("Translation service unavailable, returning original content");
            return dto;
        }
        translateObject(dto, targetLang);
        return dto;
    }

    /**
     * Translate all @Translatable String fields on each DTO in a list.
     */
    public <T> List<T> translateDtoList(List<T> dtos, String targetLang) {
        if (dtos == null || dtos.isEmpty() || targetLang == null || targetLang.isBlank() || "en".equalsIgnoreCase(targetLang)) {
            return dtos;
        }
        if (!translationService.isAvailable()) {
            log.debug("Translation service unavailable, returning original content");
            return dtos;
        }
        for (T dto : dtos) {
            translateObject(dto, targetLang);
        }
        return dtos;
    }

    /**
     * Translate specific keys in a list of Map&lt;String, String&gt; objects.
     * Useful for navigation items where DTOs are not used.
     */
    public void translateMapList(List<Map<String, String>> maps, String targetLang, String... keysToTranslate) {
        if (maps == null || maps.isEmpty() || targetLang == null || targetLang.isBlank() || "en".equalsIgnoreCase(targetLang)) {
            return;
        }
        if (!translationService.isAvailable()) {
            log.debug("Translation service unavailable, returning original content");
            return;
        }
        for (Map<String, String> map : maps) {
            for (String key : keysToTranslate) {
                String value = map.get(key);
                if (value != null && !value.isBlank()) {
                    try {
                        String translated = translationService.translatePlainText(value, "en", targetLang);
                        if (translated != null && !translated.isBlank()) {
                            map.put(key, translated);
                        }
                    } catch (Exception e) {
                        log.debug("Translation failed for map key '{}', keeping original", key);
                    }
                }
            }
        }
    }

    /**
     * Translate a batch of texts from English to a supported target language.
     * Used by the website build-time sync script.
     * Validates that targetLanguage is in the supported languages list.
     * Max 500 texts per request.
     */
    public ResponseEntity<ApiResponse<?>> translateMessages(List<String> texts, String targetLanguage) {
        if (targetLanguage == null || targetLanguage.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "targetLanguage is required"));
        }
        if (!settingsService.isLanguageSupported(targetLanguage)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Language '" + targetLanguage + "' is not in the supported languages list"));
        }
        if ("en".equalsIgnoreCase(targetLanguage)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cannot translate to the source language (en)"));
        }
        if (texts == null || texts.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "texts list is required and cannot be empty"));
        }
        if (texts.size() > 500) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Maximum 500 texts per request, received " + texts.size()));
        }
        if (!translationService.isAvailable()) {
            return ResponseEntity.status(503).body(
                    ApiResponse.error(503, "Translation service is currently unavailable"));
        }

        List<String> translations = new ArrayList<>();
        int failed = 0;
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                translations.add(text);
                continue;
            }
            try {
                String translated = translationService.translatePlainText(text, "en", targetLanguage);
                translations.add(translated != null ? translated : text);
            } catch (Exception e) {
                log.debug("Translation failed for text '{}', keeping original", text.length() > 50 ? text.substring(0, 50) + "..." : text);
                translations.add(text);
                failed++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("translations", translations);
        response.put("targetLanguage", targetLanguage);
        response.put("total", texts.size());
        response.put("failed", failed);

        return ResponseEntity.ok(ApiResponse.success(200, "Texts translated", response));
    }

    /**
     * Get supported languages with names from the translation provider.
     * Public endpoint — no authentication required.
     */
    public ResponseEntity<ApiResponse<?>> getSupportedLanguages() {
        try {
            List<String> supportedCodes = settingsService.getSupportedLanguages();

            // Get language names from the active provider
            TranslationProvider provider = providerFactory.getActiveProvider();
            List<Map<String, String>> providerLanguages = provider.getAvailableLanguages();

            // Build a code → name lookup
            Map<String, String> nameMap = new HashMap<>();
            for (Map<String, String> lang : providerLanguages) {
                nameMap.put(lang.get("code"), lang.get("name"));
            }

            // Build supported languages list with names
            List<Map<String, String>> supported = new ArrayList<>();
            for (String code : supportedCodes) {
                Map<String, String> entry = new HashMap<>();
                entry.put("code", code);
                entry.put("name", nameMap.getOrDefault(code, code.toUpperCase()));
                supported.add(entry);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("supportedLanguages", supportedCodes);
            response.put("languages", supported);
            response.put("defaultSourceLanguage", settingsService.getDefaultSourceLanguage());
            response.put("defaultTargetLanguage", settingsService.getDefaultTargetLanguage());

            return ResponseEntity.ok(ApiResponse.success(200, "Supported languages retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching supported languages", e);
            // Fallback — return just the codes from settings
            List<String> supportedCodes = settingsService.getSupportedLanguages();
            Map<String, Object> response = new HashMap<>();
            response.put("supportedLanguages", supportedCodes);
            response.put("languages", supportedCodes.stream().map(code -> {
                Map<String, String> entry = new HashMap<>();
                entry.put("code", code);
                entry.put("name", code.toUpperCase());
                return entry;
            }).toList());
            response.put("defaultSourceLanguage", settingsService.getDefaultSourceLanguage());
            response.put("defaultTargetLanguage", settingsService.getDefaultTargetLanguage());
            return ResponseEntity.ok(ApiResponse.success(200, "Supported languages retrieved (without provider names)", response));
        }
    }

    // ── Internal ──

    private void translateObject(Object obj, String targetLang) {
        if (obj == null) return;
        Class<?> clazz = obj.getClass();

        // Skip JDK/platform classes — they can't have @Translatable and reflection is blocked by modules
        String className = clazz.getName();
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("jdk.")) {
            return;
        }

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                // Translate @Translatable String fields
                if (field.isAnnotationPresent(Translatable.class) && field.getType() == String.class) {
                    String value = (String) field.get(obj);
                    if (value != null && !value.isBlank()) {
                        try {
                            String translated = translationService.translatePlainText(value, "en", targetLang);
                            if (translated != null && !translated.isBlank()) {
                                field.set(obj, translated);
                            }
                        } catch (Exception e) {
                            log.debug("Translation failed for field {}.{}, keeping original",
                                    clazz.getSimpleName(), field.getName());
                        }
                    }
                }
                // Recurse into List fields that may contain DTOs with @Translatable
                else if (Collection.class.isAssignableFrom(field.getType())) {
                    Object listVal = field.get(obj);
                    if (listVal instanceof Collection<?> collection) {
                        for (Object element : collection) {
                            if (element != null && hasTranslatableFields(element.getClass())) {
                                translateObject(element, targetLang);
                            }
                        }
                    }
                }
                // Recurse into nested DTO objects (non-primitive, non-String, non-enum)
                else if (!field.getType().isPrimitive()
                        && !field.getType().isEnum()
                        && field.getType() != String.class
                        && !Number.class.isAssignableFrom(field.getType())
                        && !Boolean.class.isAssignableFrom(field.getType())
                        && !field.getType().getName().startsWith("java.")
                        && hasTranslatableFields(field.getType())) {
                    Object nested = field.get(obj);
                    if (nested != null) {
                        translateObject(nested, targetLang);
                    }
                }
            } catch (IllegalAccessException e) {
                log.debug("Cannot access field {}.{}", clazz.getSimpleName(), field.getName());
            }
        }
    }

    /**
     * Check if a class has any @Translatable fields (direct or in nested types).
     * Results are cached per class.
     */
    private boolean hasTranslatableFields(Class<?> clazz) {
        // JDK classes never have @Translatable
        String className = clazz.getName();
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("jdk.")) {
            return false;
        }

        List<Field> cached = translatableFieldsCache.get(clazz);
        if (cached != null) return !cached.isEmpty();

        List<Field> translatable = new java.util.ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(Translatable.class)) {
                translatable.add(f);
            }
        }
        translatableFieldsCache.put(clazz, translatable);
        return !translatable.isEmpty();
    }
}
