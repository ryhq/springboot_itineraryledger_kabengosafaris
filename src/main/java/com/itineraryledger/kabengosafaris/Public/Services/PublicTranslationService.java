package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
