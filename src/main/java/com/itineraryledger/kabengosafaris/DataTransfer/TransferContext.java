package com.itineraryledger.kabengosafaris.DataTransfer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What every module needs while an import is running.
 *
 * The caches matter more than they look. A bundle of 2,600 park rates names the same handful of
 * seasons and pax categories over and over, and resolving each one by a repository lookup would be
 * thousands of identical queries — so a name resolved once stays resolved for the run.
 */
@Getter
@RequiredArgsConstructor
public class TransferContext {

    private final TransferMode mode;
    private final TransferReport report;
    /** where the bundle's `files/` were unpacked, or null when it carried none */
    private final Path files;
    private final boolean includeImages;

    /** natural key -> resolved entity, per type, for this run only */
    private final Map<String, Map<String, Object>> caches = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T cached(String type, String key, java.util.function.Function<String, T> resolve) {
        Map<String, Object> cache = caches.computeIfAbsent(type, k -> new HashMap<>());
        /*
         * containsKey rather than get() != null: "this name resolves to nothing" is worth
         * remembering too, or a bundle naming a missing lodge 300 times asks 300 times.
         */
        if (cache.containsKey(key)) return (T) cache.get(key);
        T resolved = resolve.apply(key);
        cache.put(key, resolved);
        return resolved;
    }

    public boolean mayOverwrite() {
        return mode == TransferMode.UPDATE;
    }
}
