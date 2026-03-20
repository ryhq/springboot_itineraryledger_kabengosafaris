package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLog;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailSettingGetterServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResendWebhookEventRetentionService {

    private final ResendWebhookEventRepository webhookEventRepository;
    private final EmailSettingGetterServices emailSettingGetterServices;
    private final AuditLogService auditLogService;

    /**
     * Daily cleanup at 4 AM — delete webhook events older than the email retention period
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void cleanupOldWebhookEvents() {
        try {
            Integer retentionDays = emailSettingGetterServices.getRetentionDays();
            if (retentionDays == null || retentionDays <= 0) {
                log.debug("Invalid retention days configured: {}. Skipping webhook event cleanup.", retentionDays);
                return;
            }

            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            long deletedCount = webhookEventRepository.deleteByReceivedAtBefore(cutoff);

            if (deletedCount > 0) {
                log.info("Webhook event cleanup completed. Deleted {} events older than {} days", deletedCount, retentionDays);

                auditLogService.logAction(AuditLog.builder()
                        .userId(0L)
                        .username("SYSTEM")
                        .action("CLEANUP")
                        .entityType("ResendWebhookEvent")
                        .description(String.format("Scheduled cleanup deleted %d webhook events older than %d days", deletedCount, retentionDays))
                        .status("SUCCESS")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error during webhook event cleanup", e);

            auditLogService.logAction(AuditLog.builder()
                    .userId(0L)
                    .username("SYSTEM")
                    .action("CLEANUP")
                    .entityType("ResendWebhookEvent")
                    .description("Scheduled webhook event cleanup failed: " + e.getMessage())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());
        }
    }
}
