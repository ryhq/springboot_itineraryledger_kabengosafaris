package com.itineraryledger.kabengosafaris.Security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service for preventing brute force attacks on login endpoint.
 * Tracks failed login attempts per username/email and enforces lockout periods.
 *
 * Configuration:
 * - security.rate-limit.max-attempts: Maximum failed login attempts (default: 5)
 * - security.rate-limit.lockout-duration-minutes: Lockout duration in minutes (default: 15)
 */
@Service
@Slf4j
public class RateLimitingService {

    @Value("${security.rate-limit.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.rate-limit.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    /**
     * Store login attempt records: key = username/email, value = LoginAttemptRecord
     */
    private final ConcurrentHashMap<String, LoginAttemptRecord> loginAttempts = new ConcurrentHashMap<>();

    /**
     * Record a failed login attempt for a user identifier
     *
     * @param userIdentifier username or email
     * @return number of failed attempts after recording
     */
    public int recordFailedAttempt(String userIdentifier) {
        String key = userIdentifier.toLowerCase();
        LoginAttemptRecord record = loginAttempts.computeIfAbsent(key, k -> new LoginAttemptRecord());

        // Reset if lockout period has expired
        if (record.isLockoutExpired()) {
            record.reset();
        }

        record.incrementFailedAttempts();
        log.warn("Failed login attempt for: {}. Attempts: {}/{}", key, record.getFailedAttempts(), maxFailedAttempts);

        return record.getFailedAttempts();
    }

    /**
     * Clear failed login attempts for a user (successful login)
     *
     * @param userIdentifier username or email
     */
    public void clearFailedAttempts(String userIdentifier) {
        String key = userIdentifier.toLowerCase();
        loginAttempts.remove(key);
        log.info("Cleared failed login attempts for: {}", key);
    }

    /**
     * Check if a user is currently locked out due to rate limiting
     *
     * @param userIdentifier username or email
     * @return true if user is locked out
     */
    public boolean isLockedOut(String userIdentifier) {
        String key = userIdentifier.toLowerCase();
        LoginAttemptRecord record = loginAttempts.get(key);

        if (record == null) {
            return false;
        }

        // Check if lockout has expired
        if (record.isLockoutExpired()) {
            loginAttempts.remove(key);
            return false;
        }

        // User is locked out if they've exceeded max attempts and lockout is still active
        return record.getFailedAttempts() >= maxFailedAttempts;
    }

    /**
     * Get remaining lockout time in seconds for a user
     *
     * @param userIdentifier username or email
     * @return remaining lockout time in seconds, or 0 if not locked out
     */
    public long getRemainingLockoutTimeSeconds(String userIdentifier) {
        String key = userIdentifier.toLowerCase();
        LoginAttemptRecord record = loginAttempts.get(key);

        if (record == null || record.isLockoutExpired()) {
            return 0;
        }

        long remainingMillis = record.getLockoutExpirationTime() - System.currentTimeMillis();
        return Math.max(0, remainingMillis / 1000);
    }

    /**
     * Get current failed attempt count for a user
     *
     * @param userIdentifier username or email
     * @return number of failed attempts
     */
    public int getFailedAttemptCount(String userIdentifier) {
        String key = userIdentifier.toLowerCase();
        LoginAttemptRecord record = loginAttempts.get(key);

        if (record == null) {
            return 0;
        }

        if (record.isLockoutExpired()) {
            loginAttempts.remove(key);
            return 0;
        }

        return record.getFailedAttempts();
    }

    /**
     * Inner class to track login attempts per user
     */
    private class LoginAttemptRecord {
        private int failedAttempts;
        private long lockoutExpirationTime;

        LoginAttemptRecord() {
            this.failedAttempts = 0;
            this.lockoutExpirationTime = 0;
        }

        void incrementFailedAttempts() {
            this.failedAttempts++;
            // Set lockout expiration time when max attempts is reached
            if (this.failedAttempts >= maxFailedAttempts) {
                this.lockoutExpirationTime = System.currentTimeMillis() + (lockoutDurationMinutes * 60 * 1000L);
                log.warn("User locked out until: {} ({})", new java.util.Date(lockoutExpirationTime),
                    lockoutDurationMinutes + " minutes from now");
            }
        }

        int getFailedAttempts() {
            return failedAttempts;
        }

        long getLockoutExpirationTime() {
            return lockoutExpirationTime;
        }

        boolean isLockoutExpired() {
            return lockoutExpirationTime > 0 && System.currentTimeMillis() > lockoutExpirationTime;
        }

        void reset() {
            this.failedAttempts = 0;
            this.lockoutExpirationTime = 0;
        }
    }
}
