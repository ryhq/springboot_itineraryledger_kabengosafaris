package com.itineraryledger.kabengosafaris.User.Services.LoginServices;

import com.itineraryledger.kabengosafaris.Security.SecuritySettingsService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service for login attempts using token bucket algorithm.
 * Uses database-driven settings from SecuritySettingsService for dynamic configuration.
 *
 * Database settings used:
 * - loginAttempts.maxCapacity: Maximum login attempts (burst capacity)
 * - loginAttempts.refillRate: Number of tokens to refill
 * - loginAttempts.refillDurationMinutes: Duration in minutes for refill
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitingService {

    private final SecuritySettingsService securitySettingsService;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check if a login attempt is allowed for the given username.
     * Returns true if the user has remaining tokens, false if rate limit is exceeded.
     *
     * @param username the username attempting to login
     * @return true if login attempt is allowed, false if rate limited
     */
    public boolean isAllowed(String username) {
        try {
            Bucket bucket = buckets.computeIfAbsent(username, this::createNewBucket);
            boolean allowed = bucket.tryConsume(1);

            if (!allowed) {
                log.warn("Login rate limit exceeded for username: {}", username);
            }
            return allowed;
        } catch (Exception e) {
            log.error("Error checking rate limit for username: {}", username, e);
            // Fail open - allow login attempt on configuration error
            return true;
        }
    }

    /**
     * Creates a new token bucket with database-driven settings.
     * In practice:
     * - User starts with tokens equal to maxCapacity (can make requests immediately)
     * - After using all tokens, they must wait
     * - After refillDurationMinutes, they get refillRate tokens back
     *
     * @param username the username to create bucket for
     * @return a new Bucket configured with database settings
     */
    private Bucket createNewBucket(String username) {
        try {
            int capacity = getLoginAttemptMaxCapacity();
            int refillRate = getLoginAttemptRefillRate();
            int refillDurationMinutes = getLoginAttemptRefillDurationMinutes();

            Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillRate, Duration.ofMinutes(refillDurationMinutes))
                .build();

            Bucket bucket = Bucket.builder().addLimit(limit).build();
            log.debug("Created rate limit bucket for username: {} with capacity: {}, refill: {}/{} minutes",
                username, capacity, refillRate, refillDurationMinutes);

            return bucket;
        } catch (Exception e) {
            log.error("Error creating rate limit bucket for username: {}, using defaults", username, e);
            // Fallback to default values if database settings fail
            return createDefaultBucket();
        }
    }

    /**
     * Creates a bucket with default hardcoded values.
     * Used as fallback if database settings cannot be retrieved.
     *
     * @return a Bucket with default configuration
     */
    private Bucket createDefaultBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(5)
            .refillIntervally(5, Duration.ofMinutes(1))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Get maximum login attempt capacity from database settings.
     *
     * @return maximum capacity (default: 5)
     */
    private int getLoginAttemptMaxCapacity() {
        return securitySettingsService.getSettingValueAsInteger("loginAttempts.maxCapacity");
    }

    /**
     * Get login attempt refill rate from database settings.
     *
     * @return refill rate (default: 5)
     */
    private int getLoginAttemptRefillRate() {
        return securitySettingsService.getSettingValueAsInteger("loginAttempts.refillRate");
    }

    /**
     * Get login attempt refill duration in minutes from database settings.
     *
     * @return refill duration in minutes (default: 1)
     */
    private int getLoginAttemptRefillDurationMinutes() {
        return securitySettingsService.getSettingValueAsInteger("loginAttempts.refillDurationMinutes");
    }
}
