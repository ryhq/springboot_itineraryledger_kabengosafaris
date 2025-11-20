package com.itineraryledger.kabengosafaris.User.Services;

import com.itineraryledger.kabengosafaris.Security.SecuritySettingsService;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled maintenance service for account lockout and failed attempt management.
 * Runs periodically to:
 * - Automatically unlock accounts whose lockout period has expired
 * - Reset failed attempt counters for users who have been inactive
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountMaintenanceScheduledService {

    private final UserRepository userRepository;
    private final SecuritySettingsService securitySettingsService;

    /**
     * Automatically unlocks accounts whose lockout period has expired.
     * Runs every 5 minutes.
     *
     * Process:
     * 1. Fetch all locked accounts with accountLockedTime set
     * 2. For each account, check if lockoutDurationMinutes has passed since lock time
     * 3. If expired, unlock the account and reset failed attempts
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void unlockExpiredAccounts() {
        try {
            int lockoutDurationMinutes = securitySettingsService.getSettingValueAsInteger("accountLockout.lockoutDurationMinutes");

            List<User> lockedUsers = userRepository.findByAccountLockedTrue();

            for (User user : lockedUsers) {
                if (user.getAccountLockedTime() == null) {
                    continue;
                }

                LocalDateTime unlockTime = user.getAccountLockedTime().plusMinutes(lockoutDurationMinutes);

                if (LocalDateTime.now().isAfter(unlockTime)) {
                    user.setAccountLocked(false);
                    user.setAccountLockedTime(null);
                    user.setFailedAttempt(0);
                    user.setLastFailedAttemptTime(null);
                    userRepository.save(user);
                    log.info("Account automatically unlocked for user: {}", user.getUsername());
                }
            }
        } catch (Exception e) {
            log.error("Error in unlockExpiredAccounts scheduled task", e);
        }
    }

    /**
     * Resets failed attempt counters for users who have been inactive.
     * Runs every 10 minutes.
     *
     * Process:
     * 1. Fetch all users with failed attempts > 0
     * 2. For each user, check if counterResetHours has passed since last failed attempt
     * 3. If expired, reset the failed attempt counter
     */
    @Scheduled(fixedRate = 600000) // Run every 10 minutes
    @Transactional
    public void resetExpiredFailedAttemptCounters() {
        try {
            int counterResetHours = securitySettingsService.getSettingValueAsInteger("accountLockout.counterResetHours");

            if (counterResetHours == 0) {
                log.debug("Counter reset disabled (counterResetHours = 0), skipping scheduled reset");
                return;
            }

            List<User> usersWithFailedAttempts = userRepository.findByFailedAttemptGreaterThan(0);

            for (User user : usersWithFailedAttempts) {
                if (user.getLastFailedAttemptTime() == null) {
                    continue;
                }

                LocalDateTime resetDeadline = user.getLastFailedAttemptTime().plusHours(counterResetHours);

                if (LocalDateTime.now().isAfter(resetDeadline)) {
                    user.setFailedAttempt(0);
                    user.setLastFailedAttemptTime(null);
                    userRepository.save(user);
                    log.info("Failed attempt counter reset for user: {}", user.getUsername());
                }
            }
        } catch (Exception e) {
            log.error("Error in resetExpiredFailedAttemptCounters scheduled task", e);
        }
    }
}
