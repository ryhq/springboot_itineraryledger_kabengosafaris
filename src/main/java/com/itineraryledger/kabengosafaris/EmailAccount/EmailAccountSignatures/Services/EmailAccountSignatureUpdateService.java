package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.EmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.UpdateEmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailSignatureUpdateService - Service for updating email signatures
 *
 * Responsibilities:
 * - Update signature content on disk
 * - Update signature metadata in database
 * - Handle default signature logic (only one per account)
 * - Toggle enabled/disabled status
 * - Manage variable definitions
 */
@Service
@Slf4j
@Transactional
public class EmailAccountSignatureUpdateService {

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private EmailAccountSignatureRepository emailAccountSignatureRepository;

    @Autowired
    private EmailAccountSignatureService emailAccountSignatureService;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Update a signature
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param signatureIdObfuscated Obfuscated signature ID
     * @param updateDTO The DTO with fields to update (only provided fields will be updated)
     * @return ResponseEntity with updated signature or error
     */
    public ResponseEntity<ApiResponse<?>> updateSignature(String emailAccountIdObfuscated, String signatureIdObfuscated,
            UpdateEmailAccountSignatureDTO updateDTO) {
        log.info("Updating signature: {} for account: {}", signatureIdObfuscated, emailAccountIdObfuscated);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);
            Long signatureId = idObfuscator.decodeId(signatureIdObfuscated);

            // Verify email account exists
            EmailAccount emailAccount = emailAccountRepository.findById(emailAccountId).orElse(null);
            if (emailAccount == null) {
                log.warn("Email account not found: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND")
                );
            }

            // Find signature
            EmailAccountSignature signature = emailAccountSignatureRepository.findById(signatureId).orElse(null);
            if (signature == null || !signature.getEmailAccount().getId().equals(emailAccountId)) {
                log.warn("Signature not found or does not belong to account: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND")
                );
            }

            String oldFileName = signature.getFileName();
            String currentName = signature.getName();
            String newFileName = oldFileName;

            // Update name if provided and validate uniqueness
            if (updateDTO.getName() != null && !updateDTO.getName().isBlank()) {
                // Check if name is different from current name
                if (!updateDTO.getName().equals(currentName)) {
                    // Validate that new name is unique for this account
                    if (emailAccountSignatureRepository.existsByEmailAccountIdAndName(emailAccountId, updateDTO.getName())) {
                        log.warn("Signature name already exists for account: {} with name: {}", emailAccountId, updateDTO.getName());
                        return ResponseEntity.badRequest().body(
                                ApiResponse.error(
                                        400,
                                        "Signature with this name already exists for this account",
                                        "SIGNATURE_ALREADY_EXISTS"
                                )
                        );
                    }

                    // Generate new filename with updated name
                    newFileName = emailAccountSignatureService.generateFileName(emailAccount.getName(), updateDTO.getName());
                    log.debug("Name updated from {} to {}, filename will change from {} to {}",
                            currentName, updateDTO.getName(), oldFileName, newFileName);

                    // Read current content from old file
                    String currentContent = emailAccountSignatureService.readSignatureFile(oldFileName);
                    if (currentContent == null) {
                        log.error("Failed to read existing signature file: {}", oldFileName);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiResponse.error(500, "Failed to read existing signature file", "SIGNATURE_FILE_READ_FAILED")
                        );
                    }

                    // Save content to new filename
                    boolean savedNew = emailAccountSignatureService.saveSignatureFile(currentContent, newFileName);
                    if (!savedNew) {
                        log.error("Failed to save signature file with new name: {}", newFileName);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiResponse.error(500, "Failed to save signature file with new name", "SIGNATURE_FILE_SAVE_FAILED")
                        );
                    }

                    // Delete old file
                    boolean deletedOld = emailAccountSignatureService.deleteSignatureFile(oldFileName);
                    if (!deletedOld) {
                        log.warn("Failed to delete old signature file: {}", oldFileName);
                    }

                    // Update signature entity with new name and filename
                    signature.setName(updateDTO.getName());
                    signature.setFileName(newFileName);

                    // Update file size
                    long fileSize = emailAccountSignatureService.getFileSize(newFileName);
                    signature.setFileSize(fileSize);
                }
            }

            // Update content if provided
            if (updateDTO.getContent() != null && !updateDTO.getContent().isBlank()) {
                boolean updated = emailAccountSignatureService.updateSignatureFile(signature.getFileName(), updateDTO.getContent());
                if (!updated) {
                    log.error("Failed to update signature file: {}", signature.getFileName());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to update signature file", "SIGNATURE_FILE_UPDATE_FAILED")
                    );
                }
                // Update file size
                long fileSize = emailAccountSignatureService.getFileSize(signature.getFileName());
                signature.setFileSize(fileSize);
            }

            // Update description if provided
            if (updateDTO.getDescription() != null) {
                signature.setDescription(updateDTO.getDescription());
            }

            // Update variables if provided
            if (updateDTO.getVariables() != null) {
                String variablesJson = emailAccountSignatureService.variablesToJson(updateDTO.getVariables());
                signature.setVariablesJson(variablesJson);
            }

            // Update enabled status if provided
            if (updateDTO.getEnabled() != null) {
                signature.setEnabled(updateDTO.getEnabled());
            }

            // Update default status if provided
            if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault()) {
                // Clear all other defaults for this account
                emailAccountSignatureRepository.clearAllDefaults(emailAccountId);
                signature.setIsDefault(true);
                log.info("Signature {} set as default for account {}", signatureId, emailAccountId);
            } else if (updateDTO.getIsDefault() != null) {
                signature.setIsDefault(false);
            }

            // Save updated signature
            EmailAccountSignature updated = emailAccountSignatureRepository.save(signature);

            // Build DTO with content included
            String content = emailAccountSignatureService.readSignatureFile(updated.getFileName());
            EmailAccountSignatureDTO signatureDTO = EmailAccountSignatureDTO.builder()
                    .id(idObfuscator.encodeId(updated.getId()))
                    .emailAccountId(idObfuscator.encodeId(updated.getEmailAccount().getId()))
                    .name(updated.getName())
                    .description(updated.getDescription())
                    .content(content)
                    .fileName(updated.getFileName())
                    .isDefault(updated.getIsDefault())
                    .enabled(updated.getEnabled())
                    .isSystemDefault(updated.getIsSystemDefault())
                    .variables(emailAccountSignatureService.parseVariablesJson(updated.getVariablesJson()))
                    .fileSize(updated.getFileSize())
                    .createdAt(updated.getCreatedAt())
                    .updatedAt(updated.getUpdatedAt())
                    .build();

            log.info("Signature updated successfully: {}", signatureId);

            return ResponseEntity.ok(ApiResponse.success(200, "Signature updated successfully", signatureDTO));

        } catch (Exception e) {
            log.error("Error updating signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update signature", "SIGNATURE_UPDATE_FAILED")
            );
        }
    }

    /**
     * Restore system default signature to its original template
     * This endpoint allows users to reset a modified system default signature back to the original default template
     *
     * @param emailAccountIdObfuscated Obfuscated email account ID
     * @param signatureIdObfuscated Obfuscated signature ID
     * @return ResponseEntity with updated signature or error
     */
    public ResponseEntity<ApiResponse<?>> restoreSystemDefaultSignature(String emailAccountIdObfuscated, String signatureIdObfuscated) {
        log.info("Restoring system default signature: {} for account: {}", signatureIdObfuscated, emailAccountIdObfuscated);

        try {
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);
            Long signatureId = idObfuscator.decodeId(signatureIdObfuscated);

            // Verify email account exists
            EmailAccount emailAccount = emailAccountRepository.findById(emailAccountId).orElse(null);
            if (emailAccount == null) {
                log.warn("Email account not found: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND")
                );
            }

            // Find signature
            EmailAccountSignature signature = emailAccountSignatureRepository.findById(signatureId).orElse(null);
            if (signature == null || !signature.getEmailAccount().getId().equals(emailAccountId)) {
                log.warn("Signature not found or does not belong to account: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND")
                );
            }

            // Verify that this is a system default signature
            if (!Boolean.TRUE.equals(signature.getIsSystemDefault())) {
                log.warn("Signature is not a system default signature: {}", signatureId);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Signature is not a system default signature", "NOT_SYSTEM_DEFAULT_SIGNATURE")
                );
            }

            // Generate default signature template
            String defaultContent = emailAccountSignatureService.generateDefaultSignatureTemplate(
                emailAccount.getName(),
                emailAccount.getEmail()
            );

            // Update signature file on disk
            boolean updated = emailAccountSignatureService.updateSignatureFile(signature.getFileName(), defaultContent);
            if (!updated) {
                log.error("Failed to update signature file: {}", signature.getFileName());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.error(500, "Failed to update signature file", "SIGNATURE_FILE_UPDATE_FAILED")
                );
            }

            // Update file size
            long fileSize = emailAccountSignatureService.getFileSize(signature.getFileName());
            signature.setFileSize(fileSize);

            // Reset description to default
            signature.setDescription("System default signature - created automatically");

            // Reset variables to empty
            signature.setVariablesJson("[]");

            // Save updated signature
            EmailAccountSignature updatedSignature = emailAccountSignatureRepository.save(signature);

            // Build DTO with content included
            String content = emailAccountSignatureService.readSignatureFile(updatedSignature.getFileName());
            EmailAccountSignatureDTO signatureDTO = EmailAccountSignatureDTO.builder()
                    .id(idObfuscator.encodeId(updatedSignature.getId()))
                    .emailAccountId(idObfuscator.encodeId(updatedSignature.getEmailAccount().getId()))
                    .name(updatedSignature.getName())
                    .description(updatedSignature.getDescription())
                    .content(content)
                    .fileName(updatedSignature.getFileName())
                    .isDefault(updatedSignature.getIsDefault())
                    .enabled(updatedSignature.getEnabled())
                    .isSystemDefault(updatedSignature.getIsSystemDefault())
                    .variables(emailAccountSignatureService.parseVariablesJson(updatedSignature.getVariablesJson()))
                    .fileSize(updatedSignature.getFileSize())
                    .createdAt(updatedSignature.getCreatedAt())
                    .updatedAt(updatedSignature.getUpdatedAt())
                    .build();

            log.info("System default signature restored successfully: {}", signatureId);

            return ResponseEntity.ok(ApiResponse.success(200, "System default signature restored successfully", signatureDTO));

        } catch (Exception e) {
            log.error("Error restoring system default signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to restore system default signature", "SIGNATURE_RESTORE_FAILED")
            );
        }
    }
}
