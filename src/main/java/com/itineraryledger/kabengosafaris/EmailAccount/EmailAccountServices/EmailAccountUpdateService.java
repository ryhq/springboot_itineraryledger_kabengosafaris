package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.UpdateEmailAccountDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.EmailAccountDTO;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailAccountUpdateService - Service for updating existing email accounts
 *
 * This service handles:
 * - Partial updates (only update provided fields)
 * - Enum conversion for provider types
 * - SMTP password encryption
 * - Duplicate email and name validation
 * - Default account logic (only one can be default)
 * - Optional SMTP connection verification
 * - Response formatting with full EmailAccountDTO
 */
@Service
@Slf4j
@Transactional
public class EmailAccountUpdateService {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailAccountGetService emailAccountGetService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public EmailAccountUpdateService(
            EmailAccountRepository emailAccountRepository,
            EmailAccountGetService emailAccountGetService,
            IdObfuscator idObfuscator) {
        this.emailAccountRepository = emailAccountRepository;
        this.emailAccountGetService = emailAccountGetService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an existing email account with partial updates
     *
     * @param idObfuscated The obfuscated email account ID
     * @param updateDTO The DTO containing fields to update (only provided fields will be updated)
     * @return ResponseEntity with ApiResponse containing updated account or error
     */
    public ResponseEntity<ApiResponse<?>> updateEmailAccount(String idObfuscated, UpdateEmailAccountDTO updateDTO) {
        log.info("Updating email account with ID: {}", idObfuscated);

        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            return updateEmailAccount(updateDTO, id);

        } catch (Exception e) {
            log.error("Error updating email account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500, 
                    "Failed to update email account", 
                    "EMAIL_ACCOUNT_UPDATE_FAILED"
                )
            );
        }
    }


    @AuditLogAnnotation(action = "UPDATE_EMAIL_ACCOUNT", description = "Updating an email account", entityType = "EmailAccount", entityIdParamName = "id")
    private ResponseEntity<ApiResponse<?>> updateEmailAccount(UpdateEmailAccountDTO updateDTO, Long id) {
        // Find existing account
        EmailAccount existing = emailAccountRepository.findById(id).orElse(null);

        if (existing  == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    404, 
                    "Email already exists", 
                    "DUPLICATE_EMAIL"
                )
            );
        }

        // Update email (if provided and different)
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isBlank()) {
            if (!existing.getEmail().equals(updateDTO.getEmail())) {
                // Check for duplicate email
                if (emailAccountRepository.findByEmail(updateDTO.getEmail()).isPresent()) {
                    log.warn("Email already exists: {}", updateDTO.getEmail());
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400, 
                            "Email already exists", 
                            "DUPLICATE_EMAIL"
                        )
                    );
                }
                existing.setEmail(updateDTO.getEmail());
            }
        }

        // Update name (if provided and different)
        if (updateDTO.getName() != null && !updateDTO.getName().isBlank()) {
            if (!existing.getName().equals(updateDTO.getName())) {
                // Check for duplicate name
                if (emailAccountRepository.findByName(updateDTO.getName()).isPresent()) {
                    log.warn("Account name already exists: {}", updateDTO.getName());
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400, 
                            "Account name already exists", 
                            "DUPLICATE_NAME"
                        )
                    );
                }
                existing.setName(updateDTO.getName());
            }
        }

        // Update description
        if (updateDTO.getDescription() != null) {
            existing.setDescription(updateDTO.getDescription());
        }

        // Track if sensitive attributes changed (require re-testing)
        boolean sensitiveAttributeChanged = false;

        // Update SMTP password (encrypt if provided)
        if (updateDTO.getSmtpPassword() != null && !updateDTO.getSmtpPassword().isBlank()) {
            String encryptedPassword = EncryptionUtil.encrypt(updateDTO.getSmtpPassword());
            existing.setSmtpPassword(encryptedPassword);
            log.debug("SMTP password updated for account: {}", id);
            sensitiveAttributeChanged = true;
        }

        // Update SMTP host (if provided and different)
        if (updateDTO.getSmtpHost() != null && !updateDTO.getSmtpHost().isBlank()) {
            if (!existing.getSmtpHost().equals(updateDTO.getSmtpHost())) {
                existing.setSmtpHost(updateDTO.getSmtpHost());
                sensitiveAttributeChanged = true;
            }
        }

        // Update SMTP port (if provided and different)
        if (updateDTO.getSmtpPort() != null) {
            if (!existing.getSmtpPort().equals(updateDTO.getSmtpPort())) {
                existing.setSmtpPort(updateDTO.getSmtpPort());
                sensitiveAttributeChanged = true;
            }
        }

        // Update SMTP username (if provided and different)
        if (updateDTO.getSmtpUsername() != null && !updateDTO.getSmtpUsername().isBlank()) {
            if (!existing.getSmtpUsername().equals(updateDTO.getSmtpUsername())) {
                existing.setSmtpUsername(updateDTO.getSmtpUsername());
                sensitiveAttributeChanged = true;
            }
        }

        // Update security settings (if provided and different)
        if (updateDTO.getUseTls() != null) {
            if (!existing.getUseTls().equals(updateDTO.getUseTls())) {
                existing.setUseTls(updateDTO.getUseTls());
                sensitiveAttributeChanged = true;
            }
        }
        if (updateDTO.getUseSsl() != null) {
            if (!existing.getUseSsl().equals(updateDTO.getUseSsl())) {
                existing.setUseSsl(updateDTO.getUseSsl());
                sensitiveAttributeChanged = true;
            }
        }

        // If sensitive attributes changed, disable and clear default status
        if (sensitiveAttributeChanged) {
            existing.setEnabled(false);
            existing.setIsDefault(false);
            log.info("Sensitive attributes changed for account {}. Account disabled and default status cleared. Requires re-testing.", id);
        }

        // Update enabled status
        if (updateDTO.getEnabled() != null) {
            existing.setEnabled(updateDTO.getEnabled());
        }

        // Update default status (if set to true, unset all others)
        if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault()) {
            existing.setIsDefault(true);
            emailAccountRepository.setOnlyOneDefault(id);
            log.info("Account {} set as default, all others unset", id);
        } else if (updateDTO.getIsDefault() != null) {
            existing.setIsDefault(false);
        }

        // Update provider type (if provided)
        if (updateDTO.getProviderType() != null) {
            EmailAccountProvider providerType = validateAndGetProviderType(updateDTO.getProviderType());
            if (providerType == null) {
                log.warn("Invalid provider type: {}", updateDTO.getProviderType());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400, 
                        "Invalid provider type", 
                        "INVALID_PROVIDER_TYPE"
                    )
                );
            }
            existing.setProviderType(providerType);
        }

        // Update retry and rate limiting settings
        if (updateDTO.getRateLimitPerMinute() != null) {
            existing.setRateLimitPerMinute(updateDTO.getRateLimitPerMinute());
        }
        if (updateDTO.getMaxRetryAttempts() != null) {
            existing.setMaxRetryAttempts(updateDTO.getMaxRetryAttempts());
        }
        if (updateDTO.getRetryDelaySeconds() != null) {
            existing.setRetryDelaySeconds(updateDTO.getRetryDelaySeconds());
        }

        // Save updated account
        EmailAccount updated = emailAccountRepository.save(existing);

        log.info("Email account updated successfully: {}", id);

        // Convert to DTO and return
        EmailAccountDTO emailAccountDTO = emailAccountGetService.convertToDTO(updated);

        return ResponseEntity.ok().body(
            ApiResponse.success(200,
                "Email account updated successfully",
                emailAccountDTO
            )
        );
    }

    /**
     * Toggle email account default status
     * Only allows setting as default if account is enabled (has passed test)
     *
     * @param idObfuscated The obfuscated email account ID
     * @param setAsDefault Whether to set as default (true) or unset (false)
     * @return ResponseEntity with ApiResponse containing updated account
     */
    public ResponseEntity<ApiResponse<?>> toggleDefaultStatus(String idObfuscated, boolean setAsDefault) {
        log.info("Toggling default status for email account with ID: {}, setAsDefault: {}", idObfuscated, setAsDefault);

        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            return toggleDefaultStatus(setAsDefault, id);

        } catch (Exception e) {
            log.error("Error toggling default status for email account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to toggle default status", "TOGGLE_DEFAULT_FAILED"));
        }
    }

    @AuditLogAnnotation(action = "TOGGLE_DEFAULT_EMAIL_ACCOUNT", description = "Toggling email account default status", entityType = "EmailAccount" , entityIdParamName = "id")
    private ResponseEntity<ApiResponse<?>> toggleDefaultStatus(boolean setAsDefault, Long id) {
        EmailAccount emailAccount = emailAccountRepository.findById(id).orElse(null);

        if (emailAccount  == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    404, 
                    "Email account not found", 
                    "EMAIL_NOT_FOUND"
                )
            );
        }

        // If setting as default, validate that account is enabled
        if (setAsDefault) {
            if (!Boolean.TRUE.equals(emailAccount.getEnabled())) {
                log.warn("Cannot set disabled account {} as default. Must enable and test first.", id);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Account must be enabled before setting as default. Please test the connection first.",
                                "ACCOUNT_NOT_ENABLED"));
            }

            // Set as default and unset all others
            emailAccount.setIsDefault(true);
            emailAccountRepository.setOnlyOneDefault(id);
            log.info("Account {} set as default, all others unset", id);
        } else {
            // Unsetting as default
            emailAccount.setIsDefault(false);
            log.info("Account {} unset as default", id);
        }

        // Save updated account
        EmailAccount updated = emailAccountRepository.save(emailAccount);

        // Convert to DTO and return
        EmailAccountDTO emailAccountDTO = emailAccountGetService.convertToDTO(updated);

        String message = setAsDefault ?
                "Email account set as default successfully." :
                "Email account default status removed successfully.";

        return ResponseEntity.ok().body(
                ApiResponse.success(200, message, emailAccountDTO));
    }

    /**
     * Convert provider type integer to EmailAccountProvider enum
     *
     * @param providerTypeInt The provider type as integer
     * @return EmailAccountProvider enum or null if invalid
     */
    private EmailAccountProvider validateAndGetProviderType(Integer providerTypeInt) {
        if (providerTypeInt == null) {
            return null;
        }

        switch (providerTypeInt) {
            case 1:
                return EmailAccountProvider.GMAIL;
            case 2:
                return EmailAccountProvider.OUTLOOK;
            case 3:
                return EmailAccountProvider.SENDGRID;
            case 4:
                return EmailAccountProvider.MAILGUN;
            case 5:
                return EmailAccountProvider.AWS_SES;
            case 6:
                return EmailAccountProvider.CUSTOM;
            default:
                return null;
        }
    }
}
