package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureService;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailAccountDeleteService - Service for deleting email accounts
 *
 * This service handles:
 * - Single or batch deletion of email accounts by obfuscated IDs
 * - Validation preventing deletion of default accounts
 * - Atomic operation: if any account in the list is default, no accounts are deleted
 * - Deletion of associated signature files from disk
 * - Cascade deletion of signature records (via JPA cascade rules)
 * - Response formatting with list of deleted IDs
 */
@Service
@Slf4j
@Transactional
public class EmailAccountDeleteService {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailAccountSignatureRepository emailAccountSignatureRepository;
    private final EmailAccountSignatureService emailAccountSignatureService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public EmailAccountDeleteService(
            EmailAccountRepository emailAccountRepository,
            EmailAccountSignatureRepository emailAccountSignatureRepository,
            EmailAccountSignatureService emailAccountSignatureService,
            IdObfuscator idObfuscator) {
        this.emailAccountRepository = emailAccountRepository;
        this.emailAccountSignatureRepository = emailAccountSignatureRepository;
        this.emailAccountSignatureService = emailAccountSignatureService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete email accounts by list of obfuscated IDs
     *
     * Validation: If any account in the list is default, no accounts will be deleted
     * Returns list of successfully deleted IDs
     *
     * @param idObfuscatedList List of obfuscated email account IDs
     * @return ResponseEntity with ApiResponse containing list of deleted IDs
     */
    public ResponseEntity<ApiResponse<?>> deleteEmailAccounts(List<String> idObfuscatedList) {
        log.info("Deleting {} email accounts", idObfuscatedList.size());

        try {
            // Decode all obfuscated IDs
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteEmailAccountsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting email accounts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            500,
                            "Failed to delete email accounts",
                            "EMAIL_ACCOUNTS_DELETE_FAILED"
                    )
            );
        }
    }


    private ResponseEntity<ApiResponse<?>> deleteEmailAccountsInternal(List<Long> ids) {
        // First, validate that no account in the list is default
        List<Long> defaultAccountIds = new ArrayList<>();

        for (Long id : ids) {
            EmailAccount emailAccount = emailAccountRepository.findById(id).orElse(null);

            if (emailAccount != null && Boolean.TRUE.equals(emailAccount.getIsDefault())) {
                defaultAccountIds.add(id);
            }
        }

        // If any default accounts found, reject entire operation
        if (!defaultAccountIds.isEmpty()) {
            log.warn("Cannot delete: {} account(s) in the list are default email account(s)", defaultAccountIds.size());

            Map<String, Object> result = new HashMap<>();
            result.put("deletedIds", new ArrayList<Long>());
            result.put("message", "Cannot delete any accounts: " + defaultAccountIds.size() + " account(s) in the list are set as default. Please change the default account first.");
            result.put("defaultAccountIds", defaultAccountIds);

            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            400,
                            "Cannot delete accounts: some are set as default",
                            "CANNOT_DELETE_DEFAULT_ACCOUNTS"
                    )
            );
        }

        for (Long id : ids) {
            try {
                EmailAccount emailAccount = emailAccountRepository.findById(id).orElse(null);

                if (emailAccount == null) {
                    log.warn("Email account not found: {}", id);
                    continue;
                }

                // Delete all associated signature files before deleting the account
                deleteSignatureFilesForAccount(id);

                deleteEmailAccount(id);
                log.info("Email account deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting email account: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
                ApiResponse.success(
                        200,
                        "Email accounts deleted successfully",
                        null
                )
        );
    }

    @AuditLogAnnotation(action = "DELETE_EMAIL_ACCOUNTS", description = "Deleting email accounts", entityType = "EmailAccount", entityIdParamName = "id")
    private void deleteEmailAccount(Long id) {
        emailAccountRepository.deleteById(id);
    }

    /**
     * Delete all signature files associated with an email account
     * Files are deleted from disk; database records are deleted via cascade
     *
     * @param emailAccountId The email account ID
     */
    private void deleteSignatureFilesForAccount(Long emailAccountId) {
        try {
            // Get all signatures for this account
            List<EmailAccountSignature> signatures = emailAccountSignatureRepository.findByEmailAccountId(emailAccountId);

            // Delete each signature file from disk
            for (EmailAccountSignature signature : signatures) {
                try {
                    boolean deleted = emailAccountSignatureService.deleteSignatureFile(signature.getFileName());
                    if (deleted) {
                        log.debug("Signature file deleted: {}", signature.getFileName());
                    } else {
                        log.warn("Failed to delete signature file: {}", signature.getFileName());
                    }
                } catch (Exception e) {
                    log.error("Error deleting signature file: {}", signature.getFileName(), e);
                }
            }

            log.info("Deleted {} signature files for email account: {}", signatures.size(), emailAccountId);

        } catch (Exception e) {
            log.error("Error deleting signature files for email account: {}", emailAccountId, e);
        }
    }
}
