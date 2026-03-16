package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailRetentionService {

    private final EmailMessageRepository emailMessageRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailMessageDeleteService emailMessageDeleteService;
    private final EmailSettingGetterServices emailSettingGetterServices;

    /**
     * Daily cleanup at 3 AM — delete old emails and auto-empty trash
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldEmails() {
        if (!Boolean.TRUE.equals(emailSettingGetterServices.isAutoCleanupEnabled())) {
            log.debug("Email auto-cleanup is disabled");
            return;
        }

        log.info("Starting email retention cleanup");

        int totalDeleted = 0;

        // 1. Auto-empty trash older than configured days
        totalDeleted += cleanupTrash();

        // 2. Delete emails older than retention period
        totalDeleted += cleanupOldEmailsByRetention();

        log.info("Email retention cleanup complete: {} messages deleted", totalDeleted);
    }

    /**
     * Delete trash emails older than trashRetentionDays
     */
    private int cleanupTrash() {
        try {
            Integer trashDays = emailSettingGetterServices.getTrashRetentionDays();
            LocalDateTime cutoff = LocalDateTime.now().minusDays(trashDays);

            List<EmailMessage> trashMessages = emailMessageRepository.findTrashOlderThan(cutoff);

            if (trashMessages.isEmpty()) {
                return 0;
            }

            log.info("Deleting {} trash messages older than {} days", trashMessages.size(), trashDays);

            for (EmailMessage message : trashMessages) {
                try {
                    emailMessageDeleteService.permanentlyDeleteInternal(message, message.getEmailAccount().getId());
                } catch (Exception e) {
                    log.warn("Failed to delete trash message {}: {}", message.getId(), e.getMessage());
                }
            }

            return trashMessages.size();
        } catch (Exception e) {
            log.error("Error cleaning up trash: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Delete emails older than retentionDays across all accounts
     */
    private int cleanupOldEmailsByRetention() {
        try {
            Integer retentionDays = emailSettingGetterServices.getRetentionDays();
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            int deleted = 0;

            List<EmailAccount> accounts = emailAccountRepository.findAll();
            for (EmailAccount account : accounts) {
                List<EmailMessage> oldMessages = emailMessageRepository
                    .findBySentAtBeforeAndFolderEmailAccountId(cutoff, account.getId());

                for (EmailMessage message : oldMessages) {
                    try {
                        emailMessageDeleteService.permanentlyDeleteInternal(message, account.getId());
                        deleted++;
                    } catch (Exception e) {
                        log.warn("Failed to delete old message {}: {}", message.getId(), e.getMessage());
                    }
                }
            }

            if (deleted > 0) {
                log.info("Deleted {} emails older than {} days", deleted, retentionDays);
            }

            return deleted;
        } catch (Exception e) {
            log.error("Error cleaning up old emails: {}", e.getMessage());
            return 0;
        }
    }
}
