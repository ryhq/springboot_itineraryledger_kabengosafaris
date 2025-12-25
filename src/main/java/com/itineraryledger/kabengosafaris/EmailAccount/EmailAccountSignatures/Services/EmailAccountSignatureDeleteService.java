package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailSignatureDeleteService - Service for deleting email signatures
 *
 * Responsibilities:
 * - Delete signatures by list of IDs (batch delete)
 * - Prevent deletion of default signatures
 * - Prevent deletion of system default signatures (can only be modified)
 * - Delete signature files from disk
 * - Atomic operation: if any signature is default or system default, no signatures are deleted
 * - Handle cascade deletion of database records
 */
@Service
@Slf4j
@Transactional
public class EmailAccountSignatureDeleteService {

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private EmailAccountSignatureRepository emailAccountSignatureRepository;

    @Autowired
    private EmailAccountSignatureService emailAccountSignatureService;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Delete signatures by list of obfuscated IDs
     *
     * Validation: If any signature in the list is default, no signatures will be deleted
     * Returns list of successfully deleted IDs
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param signatureIdObfuscatedList List of obfuscated signature IDs
     * @return ResponseEntity with ApiResponse containing list of deleted IDs
     */
    public ResponseEntity<ApiResponse<?>> deleteSignatures(String emailAccountIdObfuscated, List<String> signatureIdObfuscatedList) {
        log.info("Deleting {} signatures for account: {}", signatureIdObfuscatedList.size(), emailAccountIdObfuscated);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);

            // Verify email account exists
            EmailAccount emailAccount = emailAccountRepository.findById(emailAccountId).orElse(null);
            if (emailAccount == null) {
                log.warn("Email account not found: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(
                                404,
                                "Email account not found",
                                "EMAIL_ACCOUNT_NOT_FOUND"
                        )
                );
            }

            // Decode all obfuscated signature IDs
            List<Long> signatureIds = new ArrayList<>();
            for (String idObfuscated : signatureIdObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    signatureIds.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode signature ID: {}", idObfuscated, e);
                }
            }

            return deleteSignaturesInternal(emailAccountId, signatureIds);

        } catch (Exception e) {
            log.error("Error deleting signatures", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            500,
                            "Failed to delete signatures",
                            "SIGNATURES_DELETE_FAILED"
                    )
            );
        }
    }

    /**
     * Internal method to delete signatures by decoded IDs
     * Validates that no signature in the list is default or system default before deletion
     *
     * @param emailAccountId Email account ID
     * @param signatureIds List of signature IDs to delete
     * @return ResponseEntity with ApiResponse
     */
    private ResponseEntity<ApiResponse<?>> deleteSignaturesInternal(Long emailAccountId, List<Long> signatureIds) {
        // First, validate that no signature in the list is default or system default
        List<Long> defaultSignatureIds = new ArrayList<>();
        List<Long> systemDefaultSignatureIds = new ArrayList<>();
        List<EmailAccountSignature> signaturesToDelete = new ArrayList<>();

        for (Long signatureId : signatureIds) {
            EmailAccountSignature signature = emailAccountSignatureRepository.findById(signatureId).orElse(null);

            if (signature != null) {
                // Verify signature belongs to the email account
                if (!signature.getEmailAccount().getId().equals(emailAccountId)) {
                    log.warn("Signature {} does not belong to account {}", signatureId, emailAccountId);
                    continue;
                }

                // Check if signature is system default (cannot be deleted)
                if (Boolean.TRUE.equals(signature.getIsSystemDefault())) {
                    systemDefaultSignatureIds.add(signatureId);
                }

                // Check if signature is default
                if (Boolean.TRUE.equals(signature.getIsDefault())) {
                    defaultSignatureIds.add(signatureId);
                }

                signaturesToDelete.add(signature);
            }
        }

        // If any system default signatures found, reject entire operation
        if (!systemDefaultSignatureIds.isEmpty()) {
            log.warn("Cannot delete: {} signature(s) in the list are system default", systemDefaultSignatureIds.size());

            Map<String, Object> result = new HashMap<>();
            result.put("deletedIds", new ArrayList<String>());
            result.put("message", "Cannot delete any signatures: " + systemDefaultSignatureIds.size() + " signature(s) in the list are system default signatures. System default signatures can only be modified, not deleted.");
            result.put("systemDefaultSignatureIds", systemDefaultSignatureIds);

            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot delete signatures: some are system default",
                        "CANNOT_DELETE_SYSTEM_DEFAULT_SIGNATURES"
                    )
            );
        }

        // If any default signatures found, reject entire operation
        if (!defaultSignatureIds.isEmpty()) {
            log.warn("Cannot delete: {} signature(s) in the list are default", defaultSignatureIds.size());

            Map<String, Object> result = new HashMap<>();
            result.put("deletedIds", new ArrayList<String>());
            result.put("message", "Cannot delete any signatures: " + defaultSignatureIds.size() + " signature(s) in the list are set as default. Please change the default signature first.");
            result.put("defaultSignatureIds", defaultSignatureIds);

            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot delete signatures: some are set as default",
                        "CANNOT_DELETE_DEFAULT_SIGNATURES"
                    )
            );
        }

        // Delete signatures
        int deletedCount = 0;
        List<String> deletedFileNames = new ArrayList<>();

        for (EmailAccountSignature signature : signaturesToDelete) {
            try {
                // Delete signature file from disk
                boolean fileDeleted = emailAccountSignatureService.deleteSignatureFile(signature.getFileName());
                if (fileDeleted) {
                    log.debug("Signature file deleted: {}", signature.getFileName());
                    deletedFileNames.add(signature.getFileName());
                } else {
                    log.warn("Failed to delete signature file: {}", signature.getFileName());
                }

                // Delete signature from database
                emailAccountSignatureRepository.deleteById(signature.getId());
                deletedCount++;
                log.info("Signature deleted successfully: {}", signature.getId());

            } catch (Exception e) {
                log.error("Error deleting signature: {}", signature.getId(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("deletedFileNames", deletedFileNames);

        return ResponseEntity.ok().body(
                ApiResponse.success(
                        200,
                        "Signatures deleted successfully",
                        result
                )
        );
    }
}
