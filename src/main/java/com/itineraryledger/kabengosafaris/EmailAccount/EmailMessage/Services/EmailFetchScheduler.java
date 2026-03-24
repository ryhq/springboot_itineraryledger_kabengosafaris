package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailFetchScheduler {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailReceivingService emailReceivingService;
    private final EmailSettingGetterServices emailSettingGetterServices;

    /**
     * Lock map to prevent concurrent fetches per account
     */
    private final ConcurrentHashMap<Long, Boolean> fetchLocks = new ConcurrentHashMap<>();

    /**
     * Max backoff: 6 hours (after many consecutive failures)
     */
    private static final int MAX_BACKOFF_MINUTES = 360;

    /**
     * Max consecutive failures before disabling receiving
     */
    private static final int MAX_FAILURES_BEFORE_DISABLE = 20;

    /**
     * Periodically fetch new emails for all enabled accounts.
     * Default interval: 5 minutes (300000 ms)
     */
    @Scheduled(fixedDelayString = "${email.fetch.interval.ms:300000}")
    public void scheduledFetch() {
        if (!Boolean.TRUE.equals(emailSettingGetterServices.isAutoFetchEnabled())) {
            return;
        }

        List<EmailAccount> accounts = emailAccountRepository.findFetchEligibleAccounts();

        if (accounts.isEmpty()) {
            return;
        }

        log.debug("Scheduled email fetch for {} eligible accounts", accounts.size());

        for (EmailAccount account : accounts) {
            // Skip if not due yet (per-account interval + backoff)
            if (!isDueForFetch(account)) {
                continue;
            }

            // Skip if already fetching for this account
            if (fetchLocks.putIfAbsent(account.getId(), Boolean.TRUE) != null) {
                log.debug("Skipping account {} — fetch already in progress", account.getEmail());
                continue;
            }

            // Fetch asynchronously so one slow account doesn't block others
            fetchAsync(account);
        }
    }

    /**
     * Fetch emails for a single account asynchronously.
     */
    @Async
    public void fetchAsync(EmailAccount account) {
        try {
            int fetched = emailReceivingService.fetchNewEmails(account);
            onFetchSuccess(account, fetched);
        } catch (Exception e) {
            onFetchFailure(account, e);
        } finally {
            fetchLocks.remove(account.getId());
        }
    }

    /**
     * Check if an account is due for fetching based on its interval and backoff.
     */
    private boolean isDueForFetch(EmailAccount account) {
        if (account.getLastFetchedAt() == null) {
            return true; // Never fetched — fetch now
        }

        int intervalMinutes = account.getFetchIntervalMinutes() != null
            ? account.getFetchIntervalMinutes() : 5;

        // Apply exponential backoff for consecutive failures
        int failures = account.getConsecutiveFetchFailures() != null
            ? account.getConsecutiveFetchFailures() : 0;

        int backoffMinutes = 0;
        if (failures > 0) {
            // Backoff: interval * 2^(failures-1), capped at MAX_BACKOFF_MINUTES
            backoffMinutes = (int) Math.min(
                intervalMinutes * Math.pow(2, failures - 1),
                MAX_BACKOFF_MINUTES
            );
        }

        int effectiveInterval = Math.max(intervalMinutes, backoffMinutes);
        long minutesSinceLastFetch = ChronoUnit.MINUTES.between(account.getLastFetchedAt(), LocalDateTime.now());

        return minutesSinceLastFetch >= effectiveInterval;
    }

    /**
     * Handle successful fetch — reset failure count, update timestamps.
     */
    @Transactional
    public void onFetchSuccess(EmailAccount account, int fetchedCount) {
        account.setLastFetchedAt(LocalDateTime.now());
        account.setConsecutiveFetchFailures(0);
        account.setLastFetchErrorMessage(null);
        emailAccountRepository.save(account);

        if (fetchedCount > 0) {
            log.info("Fetched {} new emails for account {}", fetchedCount, account.getEmail());
        }
    }

    /**
     * Handle fetch failure — increment failure count, apply backoff, auto-disable after threshold.
     */
    @Transactional
    public void onFetchFailure(EmailAccount account, Exception e) {
        int failures = (account.getConsecutiveFetchFailures() != null
            ? account.getConsecutiveFetchFailures() : 0) + 1;

        account.setConsecutiveFetchFailures(failures);
        account.setLastFetchErrorMessage(e.getMessage() != null
            ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 1000))
            : "Unknown error");
        account.setLastFetchedAt(LocalDateTime.now());

        if (failures >= MAX_FAILURES_BEFORE_DISABLE) {
            account.setReceivingEnabled(false);
            log.error("Auto-disabled receiving for account {} after {} consecutive failures: {}",
                account.getEmail(), failures, e.getMessage());
        } else {
            int nextRetryMinutes = (int) Math.min(
                (account.getFetchIntervalMinutes() != null ? account.getFetchIntervalMinutes() : 5) * Math.pow(2, failures - 1),
                MAX_BACKOFF_MINUTES
            );
            log.warn("Fetch failed for account {} ({}/{} failures, next retry in ~{} min): {}",
                account.getEmail(), failures, MAX_FAILURES_BEFORE_DISABLE, nextRetryMinutes, e.getMessage());
        }

        emailAccountRepository.save(account);
    }
}
