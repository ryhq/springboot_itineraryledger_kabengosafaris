package com.itineraryledger.kabengosafaris.Response;

import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * One endpoint per module for every bulk flag change.
 *
 * Activating fifty rows used to mean fifty requests, each able to fail on its
 * own; that is slow and leaves the set half-changed with nobody the wiser. A
 * bulk call applies the flags the caller actually sent (null means "leave it")
 * and reports per-id outcomes, so the UI can say "48 updated, 2 skipped" rather
 * than a bare 200.
 *
 * Deliberately NOT a mass UPDATE query: entities go through their setters so
 * @PreUpdate hooks, auditing and validation behave exactly as on a single edit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BulkFlags {

    private final IdObfuscator idObfuscator;

    /** The shared request body. Every field is optional; only what is sent applies. */
    @Data
    public static class Request {
        private List<String> ids;
        private Boolean isActive;
        private Boolean isWebActive;
        private Boolean isBlacklisted;
        private Boolean isVip;
    }

    /**
     * Applies `mutate` to every readable id and saves it.
     *
     * @param label  what one row is, for the message ("park image")
     * @param mutate the setters to run; read the flags off the request yourself so
     *               each module only touches fields it actually has
     */
    public <T> ResponseEntity<?> apply(
        String label,
        JpaRepository<T, Long> repository,
        Request request,
        Consumer<T> mutate
    ) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No ids supplied", "NO_IDS")
            );
        }

        List<String> updatedIds = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();

        for (String obfuscated : request.getIds()) {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscated);
            } catch (Exception e) {
                skipped.add(reason(obfuscated, "Unreadable id"));
                continue;
            }

            T entity = repository.findById(id).orElse(null);
            if (entity == null) {
                skipped.add(reason(obfuscated, "No longer exists"));
                continue;
            }

            try {
                mutate.accept(entity);
                repository.save(entity);
                updatedIds.add(obfuscated);
            } catch (Exception e) {
                // one bad row must not abandon the rest of the batch
                log.warn("Bulk update failed for {} {}", label, obfuscated, e);
                skipped.add(reason(obfuscated, e.getMessage() != null ? e.getMessage() : "Could not be updated"));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("updatedCount", updatedIds.size());
        data.put("updatedIds", updatedIds);
        data.put("skipped", skipped);

        String message = updatedIds.size() + " " + label + (updatedIds.size() == 1 ? "" : "s") + " updated";
        return ResponseEntity.ok(ApiResponse.success(200, message, data));
    }

    private Map<String, String> reason(String id, String reason) {
        Map<String, String> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("reason", reason);
        return entry;
    }
}
