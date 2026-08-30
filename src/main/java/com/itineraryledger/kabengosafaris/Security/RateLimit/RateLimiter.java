package com.itineraryledger.kabengosafaris.Security.RateLimit;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Counting requests per caller, in this process.
 *
 * In memory on purpose. Every company runs its own single process against its own database — there
 * is no shared cache to put this in, and introducing one to hold a counter would be a new thing to
 * operate for no gain. The cost is honest: a restart forgives everybody, which is fine for something
 * whose job is blunting abuse rather than enforcing a quota.
 *
 * A fixed window rather than a sliding one, also on purpose. It lets a determined caller straddle
 * two windows and get up to twice the limit in a burst; the alternative keeps a timestamp per
 * request and this is not a billing system. What it must not do is leak: keys are swept once they
 * are older than their window, and the sweep is bounded so a flood cannot make the cleanup itself
 * the expensive part.
 */
@Component
@Slf4j
public class RateLimiter {

    /** one caller's count inside one window */
    private static class Window {
        final Instant startedAt;
        final AtomicInteger count = new AtomicInteger();

        Window(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** beyond this the map is swept before anything else is admitted */
    private static final int SWEEP_ABOVE = 10_000;

    /**
     * Count one request against a key.
     *
     * @return seconds to wait when the caller is over the limit, or 0 when they are not
     */
    public long check(String key, int limit, Duration window) {
        Instant now = Instant.now();

        if (windows.size() > SWEEP_ABOVE) sweep(now);

        Window current = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.startedAt.plus(window).isBefore(now)) {
                return new Window(now);
            }
            return existing;
        });

        int used = current.count.incrementAndGet();
        if (used <= limit) return 0;

        long waitFor = Duration.between(now, current.startedAt.plus(window)).getSeconds();
        return Math.max(1, waitFor);
    }

    private void sweep(Instant now) {
        int before = windows.size();
        /* an hour covers every window configured; anything older cannot still be counting */
        windows.entrySet().removeIf(entry -> entry.getValue().startedAt.plus(Duration.ofHours(1)).isBefore(now));
        log.info("Rate limiter swept {} stale counters, {} remain", before - windows.size(), windows.size());
    }

    /** for tests, and for an operator who has just locked themselves out of their own form */
    public void forget(String key) {
        windows.remove(key);
    }
}
