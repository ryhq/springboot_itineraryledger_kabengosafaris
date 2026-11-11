package com.itineraryledger.kabengosafaris.Translation.Cache;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Correcting and removing cached translations.
 *
 * <p>Why a person needs to reach into a cache at all: machine translation gets domain words
 * wrong in ways that read as fluent. "Lodge" came back as the French "loge" and the Russian
 * "ложа", both meaning a theatre box; "game drive" became "Игровой привод", a mechanical drive
 * for playing games; "single supplement" became "supplément unique", a one-off charge rather
 * than a single-occupancy one. None of those look like errors to a reader who does not know the
 * trade, and the last one is a price term a client could dispute.
 *
 * <p>A correction has to be durable to be worth making. Entries carry an expiry, and the
 * provider write path updates whatever it finds, so without protection a corrected row lapses,
 * gets re-translated, and the machine's version silently returns. That is why
 * {@link TranslationCache#isExpired()} treats a verified row as never expiring and
 * {@link TranslationCache#isProviderWritable()} keeps the provider off it, and why the delete
 * paths in the repository skip verified rows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationCacheWriteService {

    private final TranslationCacheRepository cacheRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Replace a cached translation with a human one.
     *
     * <p>Marks the row corrected AND verified: somebody typed it, so somebody stands behind it.
     * The original text and the content hash are never touched — the hash is the cache key, and
     * rewriting it would orphan the entry rather than correct it.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> correctEntry(String obfuscatedId, String translatedContent) {
        Long id = decode(obfuscatedId);
        if (id == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid cache entry id", "INVALID_ID"));
        }
        if (translatedContent == null || translatedContent.isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "A translation is required. To remove an entry, delete it.",
                    "TRANSLATION_REQUIRED"));
        }

        TranslationCache entry = cacheRepository.findById(id).orElse(null);
        if (entry == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Cache entry not found", "NOT_FOUND"));
        }

        String before = entry.getTranslatedContent();
        entry.setTranslatedContent(translatedContent.trim());
        entry.setHumanCorrected(true);
        entry.setVerified(true);
        entry.setCorrectedBy(currentUsername());
        entry.setCorrectedAt(LocalDateTime.now());
        cacheRepository.save(entry);

        log.info("Cache entry {} corrected by {} ({} -> {}): {} chars replaced",
            entry.getName(), entry.getCorrectedBy(), entry.getSourceLanguage(),
            entry.getTargetLanguage(), before == null ? 0 : before.length());

        return ResponseEntity.ok(ApiResponse.success(200,
            "Translation corrected. It will not expire and no provider will overwrite it.",
            Map.of("id", obfuscatedId, "verified", true, "humanCorrected", true)));
    }

    /**
     * Mark an entry verified, or withdraw that.
     *
     * <p>Verifying without editing is "I read it and it was already right" — which is worth
     * recording, because it is the difference between a translation nobody has checked and one
     * somebody has. Withdrawing it hands the row back to the normal expiry and provider rules.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> setVerified(String obfuscatedId, boolean verified) {
        Long id = decode(obfuscatedId);
        if (id == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid cache entry id", "INVALID_ID"));
        }
        TranslationCache entry = cacheRepository.findById(id).orElse(null);
        if (entry == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Cache entry not found", "NOT_FOUND"));
        }

        entry.setVerified(verified);
        if (verified) {
            entry.setCorrectedBy(currentUsername());
            entry.setCorrectedAt(LocalDateTime.now());
        }
        cacheRepository.save(entry);

        return ResponseEntity.ok(ApiResponse.success(200,
            verified
                ? "Marked verified. It will not expire and no provider will overwrite it."
                : "Verification withdrawn. Normal expiry and provider refresh apply again.",
            Map.of("id", obfuscatedId, "verified", verified)));
    }

    /**
     * Delete cache entries.
     *
     * <p>Reports per-id outcomes rather than a bare count, following the house delete contract,
     * because "deleted 3 of 5" with no reason for the other two is the kind of answer that gets
     * ignored. A verified entry is skipped unless {@code includeVerified} — deleting somebody's
     * correction by sweeping a filter is exactly the accident the flag exists to prevent.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteEntries(List<String> obfuscatedIds, boolean includeVerified) {
        if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No cache entries given to delete", "NO_IDS"));
        }

        List<String> deletedIds = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();

        for (String obfuscated : new LinkedHashSet<>(obfuscatedIds)) {
            Long id = decode(obfuscated);
            if (id == null) {
                skipped.add(reason(obfuscated, null, "Not a valid id"));
                continue;
            }
            TranslationCache entry = cacheRepository.findById(id).orElse(null);
            if (entry == null) {
                skipped.add(reason(obfuscated, null, "No longer exists"));
                continue;
            }
            if (Boolean.TRUE.equals(entry.getVerified()) && !includeVerified) {
                skipped.add(reason(obfuscated, entry.getName(),
                    "Verified by " + (entry.getCorrectedBy() == null ? "someone" : entry.getCorrectedBy())
                        + "; delete it on its own if you mean to discard that"));
                continue;
            }
            cacheRepository.delete(entry);
            deletedIds.add(obfuscated);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("deletedCount", deletedIds.size());
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        log.info("Deleted {} cache entries, skipped {}", deletedIds.size(), skipped.size());
        return ResponseEntity.ok(ApiResponse.success(200,
            describe(deletedIds.size(), skipped.size()), report));
    }

    private String describe(int deleted, int skipped) {
        if (skipped == 0) {
            return deleted + (deleted == 1 ? " entry deleted" : " entries deleted");
        }
        return deleted + " deleted, " + skipped + " left alone";
    }

    private Map<String, String> reason(String id, String name, String why) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name == null ? "" : name);
        m.put("reason", why);
        return m;
    }

    private Long decode(String obfuscatedId) {
        try {
            return idObfuscator.decodeId(obfuscatedId);
        } catch (Exception e) {
            return null;
        }
    }

    /** Username for the audit trail, or null when there is somehow no authenticated caller. */
    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
