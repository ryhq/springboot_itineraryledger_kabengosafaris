package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ReceivingProtocol;

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
     * Periodically fetch new emails for all enabled accounts.
     * Default interval: 5 minutes (300000 ms)
     */
    @Scheduled(fixedDelayString = "${email.fetch.interval.ms:300000}")
    public void scheduledFetch() {
        if (!Boolean.TRUE.equals(emailSettingGetterServices.isAutoFetchEnabled())) {
            return;
        }

        List<EmailAccount> accounts = emailAccountRepository.findAll().stream()
            .filter(a -> Boolean.TRUE.equals(a.getReceivingEnabled()))
            .filter(a -> a.getReceivingProtocol() != ReceivingProtocol.NONE)
            .filter(a -> Boolean.TRUE.equals(a.getEnabled()))
            .toList();

        if (accounts.isEmpty()) {
            return;
        }

        log.debug("Scheduled email fetch for {} accounts", accounts.size());

        for (EmailAccount account : accounts) {
            // Skip if already fetching for this account
            if (fetchLocks.putIfAbsent(account.getId(), Boolean.TRUE) != null) {
                log.debug("Skipping account {} — fetch already in progress", account.getEmail());
                continue;
            }

            try {
                emailReceivingService.fetchNewEmails(account);
            } catch (Exception e) {
                log.error("Scheduled fetch failed for account {}: {}", account.getEmail(), e.getMessage());
            } finally {
                fetchLocks.remove(account.getId());
            }
        }
    }
}
