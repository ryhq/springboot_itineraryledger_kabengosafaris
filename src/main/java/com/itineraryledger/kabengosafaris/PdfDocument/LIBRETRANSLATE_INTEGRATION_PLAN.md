# LibreTranslate Integration Plan for PDF Generation

## Overview

This document outlines the plan to integrate LibreTranslate (self-hosted, free translation API) with the existing PDF generation service to enable multi-language PDF document generation.

---

## Part 1: LibreTranslate Setup

### Option A: Docker Deployment (Recommended)

#### 1.1 Docker Compose Configuration

Add LibreTranslate to your existing `docker-compose.yml`:

```yaml
services:
  # ... existing services (mysql, etc.)

  libretranslate:
    image: libretranslate/libretranslate:latest
    container_name: kabengosafaris-libretranslate
    restart: unless-stopped
    ports:
      - "5000:5000"
    environment:
      - LT_LOAD_ONLY=en,fr,de,es,it,pt,nl,zh,ja,ar  # Load only needed languages
      - LT_DISABLE_WEB_UI=false  # Enable web UI for testing
      - LT_UPDATE_MODELS=true    # Auto-update language models
      - LT_SUGGESTIONS=false     # Disable suggestions API
      - LT_CHAR_LIMIT=5000       # Max chars per request (adjust as needed)
    volumes:
      - libretranslate_data:/home/libretranslate/.local/share/argos-translate
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/languages"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  libretranslate_data:
```

#### 1.2 Standalone Docker Command

```bash
# Quick start for testing
docker run -d \
  --name libretranslate \
  -p 5000:5000 \
  -e LT_LOAD_ONLY=en,fr,de,es,it \
  -v libretranslate_data:/home/libretranslate/.local/share/argos-translate \
  libretranslate/libretranslate
```

#### 1.3 Verify Installation

```bash
# Check if running
curl http://localhost:5000/languages

# Test translation
curl -X POST "http://localhost:5000/translate" \
  -H "Content-Type: application/json" \
  -d '{"q": "Hello, welcome to Tanzania Safari!", "source": "en", "target": "fr"}'
```

Expected response:
```json
{
  "translatedText": "Bonjour, bienvenue au Safari en Tanzanie !"
}
```

### Option B: Native Installation (Alternative)

```bash
# Install Python dependencies
pip install libretranslate

# Run with specific languages
libretranslate --load-only en,fr,de,es,it --port 5000
```

---

## Part 2: Supported Languages

### Languages to Configure for Safari Tourism

| Code | Language | Priority | Common Use |
|------|----------|----------|------------|
| `en` | English | High | Base language |
| `fr` | French | High | France, Belgium, Canada, Africa |
| `de` | German | High | Germany, Austria, Switzerland |
| `es` | Spanish | High | Spain, Latin America |
| `it` | Italian | Medium | Italy |
| `nl` | Dutch | Medium | Netherlands, Belgium |
| `pt` | Portuguese | Medium | Portugal, Brazil |
| `zh` | Chinese | Medium | China (growing market) |
| `ja` | Japanese | Low | Japan |
| `ar` | Arabic | Low | Middle East |

### Language Model Sizes (Approximate)

- Each language pair: ~50-100MB
- Full installation with 10 languages: ~1-2GB
- Memory usage: ~2-4GB RAM recommended

---

## Part 3: Spring Boot Integration

### 3.1 Application Configuration

Add to `application.properties`:

```properties
# LibreTranslate Configuration
libretranslate.enabled=true
libretranslate.base-url=http://localhost:5000
libretranslate.timeout=30000
libretranslate.max-chars-per-request=5000
libretranslate.default-source-language=en
libretranslate.supported-languages=en,fr,de,es,it,nl,pt,zh,ja,ar
libretranslate.cache.enabled=true
libretranslate.cache.ttl-hours=24
```

### 3.2 Configuration Class

Create: `src/main/java/com/itineraryledger/kabengosafaris/Translation/Config/LibreTranslateConfig.java`

```java
package com.itineraryledger.kabengosafaris.Translation.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "libretranslate")
public class LibreTranslateConfig {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:5000";
    private int timeout = 30000;
    private int maxCharsPerRequest = 5000;
    private String defaultSourceLanguage = "en";
    private List<String> supportedLanguages = List.of("en", "fr", "de", "es", "it");

    private CacheConfig cache = new CacheConfig();

    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private int ttlHours = 24;
    }
}
```

### 3.3 DTOs

Create: `src/main/java/com/itineraryledger/kabengosafaris/Translation/DTOs/`

```java
// TranslationRequestDTO.java
package com.itineraryledger.kabengosafaris.Translation.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationRequestDTO {
    private String text;
    private String sourceLanguage;
    private String targetLanguage;
}

// TranslationResponseDTO.java
package com.itineraryledger.kabengosafaris.Translation.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponseDTO {
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private boolean fromCache;
}

// LibreTranslateRequestDTO.java (API payload)
package com.itineraryledger.kabengosafaris.Translation.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibreTranslateRequestDTO {
    @JsonProperty("q")
    private String text;

    @JsonProperty("source")
    private String sourceLanguage;

    @JsonProperty("target")
    private String targetLanguage;

    @JsonProperty("format")
    private String format = "text"; // or "html"
}

// LibreTranslateResponseDTO.java (API response)
package com.itineraryledger.kabengosafaris.Translation.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LibreTranslateResponseDTO {
    @JsonProperty("translatedText")
    private String translatedText;
}

// LanguageDTO.java
package com.itineraryledger.kabengosafaris.Translation.DTOs;

import lombok.Data;

@Data
public class LanguageDTO {
    private String code;
    private String name;
}
```

### 3.4 Translation Cache Entity

Create: `src/main/java/com/itineraryledger/kabengosafaris/Translation/Entity/TranslationCache.java`

```java
package com.itineraryledger.kabengosafaris.Translation.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_cache", indexes = {
    @Index(name = "idx_translation_hash", columnList = "textHash,sourceLanguage,targetLanguage", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String textHash; // SHA-256 hash of original text

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Column(nullable = false, length = 5)
    private String sourceLanguage;

    @Column(nullable = false, length = 5)
    private String targetLanguage;

    @Column(nullable = false)
    private Integer characterCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private Integer hitCount = 0;
}
```

### 3.5 Repository

Create: `src/main/java/com/itineraryledger/kabengosafaris/Translation/Repository/TranslationCacheRepository.java`

```java
package com.itineraryledger.kabengosafaris.Translation.Repository;

import com.itineraryledger.kabengosafaris.Translation.Entity.TranslationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TranslationCacheRepository extends JpaRepository<TranslationCache, Long> {

    Optional<TranslationCache> findByTextHashAndSourceLanguageAndTargetLanguage(
        String textHash, String sourceLanguage, String targetLanguage
    );

    @Modifying
    @Transactional
    @Query("UPDATE TranslationCache t SET t.hitCount = t.hitCount + 1 WHERE t.id = :id")
    void incrementHitCount(Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM TranslationCache t WHERE t.expiresAt < :now")
    int deleteExpiredEntries(LocalDateTime now);

    @Query("SELECT COUNT(t) FROM TranslationCache t")
    long countCacheEntries();

    @Query("SELECT SUM(t.characterCount) FROM TranslationCache t")
    Long sumCharacterCount();
}
```

### 3.6 Translation Service

Create: `src/main/java/com/itineraryledger/kabengosafaris/Translation/Services/LibreTranslateService.java`

```java
package com.itineraryledger.kabengosafaris.Translation.Services;

import com.itineraryledger.kabengosafaris.Translation.Config.LibreTranslateConfig;
import com.itineraryledger.kabengosafaris.Translation.DTOs.*;
import com.itineraryledger.kabengosafaris.Translation.Entity.TranslationCache;
import com.itineraryledger.kabengosafaris.Translation.Repository.TranslationCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LibreTranslateService {

    private final LibreTranslateConfig config;
    private final TranslationCacheRepository cacheRepository;
    private final RestTemplate restTemplate;

    /**
     * Translate text from source to target language
     */
    public TranslationResponseDTO translate(TranslationRequestDTO request) {
        // Validate request
        validateRequest(request);

        // Check if translation is needed (same language)
        if (request.getSourceLanguage().equals(request.getTargetLanguage())) {
            return TranslationResponseDTO.builder()
                .originalText(request.getText())
                .translatedText(request.getText())
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .fromCache(false)
                .build();
        }

        // Check cache first
        if (config.getCache().isEnabled()) {
            Optional<TranslationCache> cached = getCachedTranslation(
                request.getText(),
                request.getSourceLanguage(),
                request.getTargetLanguage()
            );

            if (cached.isPresent()) {
                TranslationCache cache = cached.get();
                cacheRepository.incrementHitCount(cache.getId());
                log.debug("Translation cache hit for hash: {}", cache.getTextHash());

                return TranslationResponseDTO.builder()
                    .originalText(request.getText())
                    .translatedText(cache.getTranslatedText())
                    .sourceLanguage(request.getSourceLanguage())
                    .targetLanguage(request.getTargetLanguage())
                    .fromCache(true)
                    .build();
            }
        }

        // Call LibreTranslate API
        String translatedText = callLibreTranslateAPI(request);

        // Cache the result
        if (config.getCache().isEnabled()) {
            cacheTranslation(request, translatedText);
        }

        return TranslationResponseDTO.builder()
            .originalText(request.getText())
            .translatedText(translatedText)
            .sourceLanguage(request.getSourceLanguage())
            .targetLanguage(request.getTargetLanguage())
            .fromCache(false)
            .build();
    }

    /**
     * Translate HTML content while preserving tags
     */
    public String translateHtml(String html, String sourceLanguage, String targetLanguage) {
        if (!config.isEnabled()) {
            log.warn("LibreTranslate is disabled, returning original HTML");
            return html;
        }

        // LibreTranslate supports HTML format
        LibreTranslateRequestDTO apiRequest = LibreTranslateRequestDTO.builder()
            .text(html)
            .sourceLanguage(sourceLanguage)
            .targetLanguage(targetLanguage)
            .format("html")
            .build();

        return callLibreTranslateAPIRaw(apiRequest);
    }

    /**
     * Batch translate multiple texts
     */
    public List<TranslationResponseDTO> translateBatch(
        List<String> texts,
        String sourceLanguage,
        String targetLanguage
    ) {
        List<TranslationResponseDTO> results = new ArrayList<>();

        for (String text : texts) {
            TranslationRequestDTO request = TranslationRequestDTO.builder()
                .text(text)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .build();

            results.add(translate(request));
        }

        return results;
    }

    /**
     * Get available languages from LibreTranslate
     */
    public List<LanguageDTO> getAvailableLanguages() {
        try {
            String url = config.getBaseUrl() + "/languages";
            ResponseEntity<LanguageDTO[]> response = restTemplate.getForEntity(url, LanguageDTO[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to fetch available languages: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Check if LibreTranslate service is healthy
     */
    public boolean isHealthy() {
        try {
            String url = config.getBaseUrl() + "/languages";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("LibreTranslate health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if a language is supported
     */
    public boolean isLanguageSupported(String languageCode) {
        return config.getSupportedLanguages().contains(languageCode.toLowerCase());
    }

    // ========== Private Methods ==========

    private void validateRequest(TranslationRequestDTO request) {
        if (request.getText() == null || request.getText().isBlank()) {
            throw new IllegalArgumentException("Text to translate cannot be empty");
        }

        if (request.getText().length() > config.getMaxCharsPerRequest()) {
            throw new IllegalArgumentException(
                "Text exceeds maximum character limit of " + config.getMaxCharsPerRequest()
            );
        }

        if (!isLanguageSupported(request.getSourceLanguage())) {
            throw new IllegalArgumentException(
                "Source language not supported: " + request.getSourceLanguage()
            );
        }

        if (!isLanguageSupported(request.getTargetLanguage())) {
            throw new IllegalArgumentException(
                "Target language not supported: " + request.getTargetLanguage()
            );
        }
    }

    private String callLibreTranslateAPI(TranslationRequestDTO request) {
        LibreTranslateRequestDTO apiRequest = LibreTranslateRequestDTO.builder()
            .text(request.getText())
            .sourceLanguage(request.getSourceLanguage())
            .targetLanguage(request.getTargetLanguage())
            .format("text")
            .build();

        return callLibreTranslateAPIRaw(apiRequest);
    }

    private String callLibreTranslateAPIRaw(LibreTranslateRequestDTO apiRequest) {
        String url = config.getBaseUrl() + "/translate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LibreTranslateRequestDTO> entity = new HttpEntity<>(apiRequest, headers);

        try {
            ResponseEntity<LibreTranslateResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                LibreTranslateResponseDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getTranslatedText();
            }

            throw new RuntimeException("Translation failed with status: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("LibreTranslate API call failed: {}", e.getMessage());
            throw new RuntimeException("Translation service unavailable: " + e.getMessage(), e);
        }
    }

    private Optional<TranslationCache> getCachedTranslation(
        String text,
        String sourceLanguage,
        String targetLanguage
    ) {
        String hash = hashText(text);
        Optional<TranslationCache> cached = cacheRepository
            .findByTextHashAndSourceLanguageAndTargetLanguage(hash, sourceLanguage, targetLanguage);

        // Check if expired
        if (cached.isPresent() && cached.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        return cached;
    }

    private void cacheTranslation(TranslationRequestDTO request, String translatedText) {
        try {
            TranslationCache cache = TranslationCache.builder()
                .textHash(hashText(request.getText()))
                .originalText(request.getText())
                .translatedText(translatedText)
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .characterCount(request.getText().length())
                .expiresAt(LocalDateTime.now().plusHours(config.getCache().getTtlHours()))
                .hitCount(0)
                .build();

            cacheRepository.save(cache);
            log.debug("Cached translation for hash: {}", cache.getTextHash());

        } catch (Exception e) {
            log.warn("Failed to cache translation: {}", e.getMessage());
            // Don't fail the request if caching fails
        }
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Scheduled task to clean up expired cache entries
     */
    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    public void cleanupExpiredCache() {
        if (config.getCache().isEnabled()) {
            int deleted = cacheRepository.deleteExpiredEntries(LocalDateTime.now());
            log.info("Cleaned up {} expired translation cache entries", deleted);
        }
    }
}
```

### 3.7 RestTemplate Configuration

Create: `src/main/java/com/itineraryledger/kabengosafaris/Translation/Config/RestTemplateConfig.java`

```java
package com.itineraryledger.kabengosafaris.Translation.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(LibreTranslateConfig config) {
        return new RestTemplate(clientHttpRequestFactory(config));
    }

    private ClientHttpRequestFactory clientHttpRequestFactory(LibreTranslateConfig config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(config.getTimeout());
        return factory;
    }
}
```

---

## Part 4: PDF Translation Integration

### 4.1 Update PdfGenerationService

Modify: `src/main/java/com/itineraryledger/kabengosafaris/PdfDocument/Services/PdfGenerationService.java`

Add translation capability:

```java
// Add to existing PdfGenerationService

@Autowired
private LibreTranslateService translationService;

/**
 * Generate PDF with translation to target language
 */
public byte[] generateTranslatedPdf(
    String documentName,
    String dataId,
    String templateId,
    String targetLanguage
) {
    // 1. Generate HTML as normal
    String html = generateHtml(documentName, dataId, templateId);

    // 2. Translate HTML if needed
    if (targetLanguage != null && !targetLanguage.equals("en")) {
        if (translationService.isLanguageSupported(targetLanguage)) {
            html = translationService.translateHtml(html, "en", targetLanguage);
            log.info("Translated PDF to language: {}", targetLanguage);
        } else {
            log.warn("Language not supported for translation: {}", targetLanguage);
        }
    }

    // 3. Generate PDF from (translated) HTML
    return pdfGenerator.generatePdf(html, getTemplateOrDefault(templateId));
}

/**
 * Generate translated itinerary PDF
 */
public byte[] generateTranslatedItineraryPdf(
    Long itineraryId,
    String templateId,
    String targetLanguage
) {
    return generateTranslatedPdf(
        "FULL_ITINERARY",
        itineraryId.toString(),
        templateId,
        targetLanguage
    );
}
```

### 4.2 Update Controller

Modify: `src/main/java/com/itineraryledger/kabengosafaris/PdfDocument/Controller/PdfGenerationController.java`

```java
// Add new endpoint for translated PDFs

@PostMapping("/generate/translated")
@PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
public ResponseEntity<byte[]> generateTranslatedPdf(
    @RequestParam String documentName,
    @RequestParam String dataId,
    @RequestParam(required = false) String templateId,
    @RequestParam(defaultValue = "en") String language
) {
    byte[] pdfBytes = pdfGenerationService.generateTranslatedPdf(
        documentName, dataId, templateId, language
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename("document_" + language + ".pdf")
            .build()
    );

    return ResponseEntity.ok().headers(headers).body(pdfBytes);
}

@GetMapping("/itinerary/{itineraryId}/translated")
@PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
public ResponseEntity<byte[]> generateTranslatedItineraryPdf(
    @PathVariable Long itineraryId,
    @RequestParam(required = false) String templateId,
    @RequestParam(defaultValue = "en") String language
) {
    byte[] pdfBytes = pdfGenerationService.generateTranslatedItineraryPdf(
        itineraryId, templateId, language
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename("itinerary_" + itineraryId + "_" + language + ".pdf")
            .build()
    );

    return ResponseEntity.ok().headers(headers).body(pdfBytes);
}

@GetMapping("/languages")
public ResponseEntity<List<LanguageDTO>> getAvailableLanguages() {
    return ResponseEntity.ok(translationService.getAvailableLanguages());
}

@GetMapping("/translation/health")
public ResponseEntity<Map<String, Object>> checkTranslationHealth() {
    Map<String, Object> health = new HashMap<>();
    health.put("available", translationService.isHealthy());
    health.put("languages", translationService.getAvailableLanguages());
    return ResponseEntity.ok(health);
}
```

---

## Part 5: Database Migration

Create Flyway migration: `V{next}_create_translation_cache_table.sql`

```sql
-- Create translation cache table
CREATE TABLE translation_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text_hash VARCHAR(64) NOT NULL,
    original_text TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    source_language VARCHAR(5) NOT NULL,
    target_language VARCHAR(5) NOT NULL,
    character_count INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    hit_count INT DEFAULT 0,

    UNIQUE INDEX idx_translation_hash (text_hash, source_language, target_language),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## Part 6: Testing

### 6.1 Unit Tests

Create: `src/test/java/com/itineraryledger/kabengosafaris/Translation/LibreTranslateServiceTest.java`

```java
@SpringBootTest
@TestPropertySource(properties = {
    "libretranslate.enabled=true",
    "libretranslate.base-url=http://localhost:5000"
})
class LibreTranslateServiceTest {

    @Autowired
    private LibreTranslateService translationService;

    @Test
    void shouldTranslateSimpleText() {
        TranslationRequestDTO request = TranslationRequestDTO.builder()
            .text("Welcome to Tanzania Safari!")
            .sourceLanguage("en")
            .targetLanguage("fr")
            .build();

        TranslationResponseDTO response = translationService.translate(request);

        assertNotNull(response.getTranslatedText());
        assertNotEquals(request.getText(), response.getTranslatedText());
    }

    @Test
    void shouldCacheTranslation() {
        TranslationRequestDTO request = TranslationRequestDTO.builder()
            .text("Serengeti National Park")
            .sourceLanguage("en")
            .targetLanguage("de")
            .build();

        // First call - not from cache
        TranslationResponseDTO first = translationService.translate(request);
        assertFalse(first.isFromCache());

        // Second call - should be from cache
        TranslationResponseDTO second = translationService.translate(request);
        assertTrue(second.isFromCache());
        assertEquals(first.getTranslatedText(), second.getTranslatedText());
    }

    @Test
    void shouldTranslateHtml() {
        String html = "<h1>Welcome to Safari</h1><p>Enjoy your adventure!</p>";

        String translated = translationService.translateHtml(html, "en", "fr");

        assertNotNull(translated);
        assertTrue(translated.contains("<h1>"));
        assertTrue(translated.contains("</p>"));
    }
}
```

### 6.2 Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PdfTranslationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldGenerateTranslatedPdf() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
            "/api/pdf/itinerary/1/translated?language=fr",
            byte[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }
}
```

---

## Part 7: Implementation Checklist

### Phase 1: LibreTranslate Setup
- [ ] Add LibreTranslate to docker-compose.yml
- [ ] Configure language models to load
- [ ] Test LibreTranslate API manually
- [ ] Verify health endpoint works

### Phase 2: Spring Boot Integration
- [ ] Add configuration properties
- [ ] Create LibreTranslateConfig class
- [ ] Create DTOs (Request, Response, Language)
- [ ] Create TranslationCache entity
- [ ] Create TranslationCacheRepository
- [ ] Create LibreTranslateService
- [ ] Add RestTemplate configuration

### Phase 3: PDF Integration
- [ ] Update PdfGenerationService with translation methods
- [ ] Add new endpoints to PdfGenerationController
- [ ] Create database migration for cache table
- [ ] Run migration

### Phase 4: Testing
- [ ] Write unit tests for LibreTranslateService
- [ ] Write integration tests for PDF translation
- [ ] Manual testing with real itineraries
- [ ] Test all target languages

### Phase 5: Monitoring & Optimization
- [ ] Add logging for translation operations
- [ ] Monitor cache hit rate
- [ ] Tune cache TTL based on usage
- [ ] Add metrics for translation performance

---

## Part 8: API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/pdf/generate/translated` | Generate translated PDF |
| `GET` | `/api/pdf/itinerary/{id}/translated?language=fr` | Generate translated itinerary PDF |
| `GET` | `/api/pdf/languages` | List available languages |
| `GET` | `/api/pdf/translation/health` | Check translation service health |

---

## Part 9: Example Usage

### Generate French Itinerary PDF

```bash
curl -X GET "http://localhost:8080/api/pdf/itinerary/1/translated?language=fr" \
  -H "Authorization: Bearer {token}" \
  --output itinerary_fr.pdf
```

### Check Available Languages

```bash
curl -X GET "http://localhost:8080/api/pdf/languages"
```

Response:
```json
[
  {"code": "en", "name": "English"},
  {"code": "fr", "name": "French"},
  {"code": "de", "name": "German"},
  {"code": "es", "name": "Spanish"},
  {"code": "it", "name": "Italian"}
]
```

---

## Notes & Considerations

### Translation Quality
- LibreTranslate quality is good but not as polished as DeepL/Google
- Safari-specific terms may need manual review
- Consider a glossary for consistent translation of terms like:
  - "Game drive" → "Safari en véhicule" (FR)
  - "Big Five" → Keep as "Big Five" (marketing term)
  - Park names → Keep original names

### Performance
- First translation request per language may be slower (model loading)
- Caching significantly improves performance for repeated content
- Consider pre-translating common content during off-peak hours

### Fallback Strategy
- If LibreTranslate is unavailable, return original English content
- Log failures for monitoring
- Consider adding DeepL/Google as fallback for production

---

## Future Enhancements

1. **Glossary Support**: Define translations for safari-specific terms
2. **Pre-translation**: Batch translate itinerary templates overnight
3. **Language Detection**: Auto-detect source language
4. **Quality Review**: Flag translations for manual review
5. **Multi-provider**: Add DeepL/Google as premium translation options
