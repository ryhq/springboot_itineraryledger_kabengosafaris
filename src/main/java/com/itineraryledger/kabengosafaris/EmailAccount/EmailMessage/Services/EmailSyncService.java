package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ReceivingProtocol;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSyncService {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailReceivingService emailReceivingService;
    private final EmailFolderService emailFolderService;
    private final IdObfuscator idObfuscator;

    /**
     * Trigger a manual sync for an account
     */
    @Async
    public void triggerSync(EmailAccount account) {
        log.info("Starting manual sync for account: {}", account.getEmail());

        // Ensure folders exist
        emailFolderService.createSystemFolders(account);

        // Fetch new emails
        int fetched = emailReceivingService.fetchNewEmails(account);
        log.info("Manual sync complete for account {}: {} new emails fetched", account.getEmail(), fetched);
    }

    /**
     * Trigger sync via API (returns immediately, sync runs async)
     */
    public ResponseEntity<ApiResponse<?>> triggerSyncApi(String accountIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            if (account.getReceivingProtocol() == ReceivingProtocol.NONE) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Receiving protocol not configured for this account", "NO_RECEIVING_PROTOCOL"));
            }

            triggerSync(account);

            return ResponseEntity.ok(ApiResponse.success(200, "Sync started for account " + account.getEmail(), null));
        } catch (Exception e) {
            log.error("Error triggering sync", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to trigger sync", "SYNC_TRIGGER_FAILED"));
        }
    }

    /**
     * Get sync status for an account
     */
    public ResponseEntity<ApiResponse<?>> getSyncStatus(String accountIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("receivingProtocol", account.getReceivingProtocol());
            status.put("receivingEnabled", account.getReceivingEnabled());
            status.put("lastFetchedAt", account.getLastFetchedAt());
            status.put("lastFetchErrorMessage", account.getLastFetchErrorMessage());
            status.put("emailsReceivedCount", account.getEmailsReceivedCount());

            return ResponseEntity.ok(ApiResponse.success(200, "Sync status retrieved", status));
        } catch (Exception e) {
            log.error("Error getting sync status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get sync status", "SYNC_STATUS_FAILED"));
        }
    }
}
