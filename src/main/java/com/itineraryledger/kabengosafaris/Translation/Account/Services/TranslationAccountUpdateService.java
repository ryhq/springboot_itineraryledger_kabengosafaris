package com.itineraryledger.kabengosafaris.Translation.Account.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.TranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.UpdateTranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProviderFactory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class TranslationAccountUpdateService {

    private final TranslationAccountRepository translationAccountRepository;
    private final TranslationAccountGetService translationAccountGetService;
    private final TranslationProviderFactory providerFactory;
    private final IdObfuscator idObfuscator;

    @Autowired
    public TranslationAccountUpdateService(
            TranslationAccountRepository translationAccountRepository,
            TranslationAccountGetService translationAccountGetService,
            TranslationProviderFactory providerFactory,
            IdObfuscator idObfuscator) {
        this.translationAccountRepository = translationAccountRepository;
        this.translationAccountGetService = translationAccountGetService;
        this.providerFactory = providerFactory;
        this.idObfuscator = idObfuscator;
    }

    @AuditLogAnnotation(
        action = "UPDATE_TRANSLATION_ACCOUNT",
        description = "Updating a translation account",
        entityType = "TranslationAccount",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateTranslationAccount(String idObfuscated, UpdateTranslationAccountDTO updateDTO) {
        log.info("Updating translation account: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);

            TranslationAccount existing = translationAccountRepository.findById(id).orElse(null);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Translation account not found", "TRANSLATION_ACCOUNT_NOT_FOUND")
                );
            }

            boolean sensitiveAttributeChanged = false;

            // Update name
            if (updateDTO.getName() != null && !updateDTO.getName().isBlank()) {
                if (!existing.getName().equals(updateDTO.getName())) {
                    if (translationAccountRepository.findByName(updateDTO.getName()).isPresent()) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Account name already exists", "DUPLICATE_NAME")
                        );
                    }
                    existing.setName(updateDTO.getName());
                }
            }

            // Update description
            if (updateDTO.getDescription() != null) {
                existing.setDescription(updateDTO.getDescription());
            }

            // Update provider type
            if (updateDTO.getProviderType() != null) {
                try {
                    TranslationProviderType providerType = TranslationProviderType.fromInteger(updateDTO.getProviderType());
                    if (providerType != existing.getProviderType()) {
                        existing.setProviderType(providerType);
                        sensitiveAttributeChanged = true;
                    }
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, e.getMessage(), "INVALID_PROVIDER_TYPE")
                    );
                }
            }

            // Update API key
            if (updateDTO.getApiKey() != null && !updateDTO.getApiKey().isBlank()) {
                String encryptedApiKey = EncryptionUtil.encrypt(updateDTO.getApiKey());
                existing.setApiKey(encryptedApiKey);
                sensitiveAttributeChanged = true;
            }

            // Update base URL
            if (updateDTO.getBaseUrl() != null && !updateDTO.getBaseUrl().isBlank()) {
                if (!updateDTO.getBaseUrl().equals(existing.getBaseUrl())) {
                    existing.setBaseUrl(updateDTO.getBaseUrl());
                    sensitiveAttributeChanged = true;
                }
            }

            // Update timeout
            if (updateDTO.getTimeoutSeconds() != null) {
                existing.setTimeoutSeconds(updateDTO.getTimeoutSeconds());
            }

            // If sensitive attributes changed, disable and clear default
            if (sensitiveAttributeChanged) {
                existing.setEnabled(false);
                existing.setIsDefault(false);
                log.info("Sensitive attributes changed for account {}. Disabled and default cleared. Requires re-testing.", id);
            }

            // Update enabled (disable only)
            if (updateDTO.getEnabled() != null && !updateDTO.getEnabled()) {
                existing.setEnabled(false);
                existing.setIsDefault(false);
            }

            // Update default status
            if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault()) {
                if (!existing.getEnabled()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Untested translation account cannot be set as default.", "UNTESTED_TRANSLATION_CONNECTION")
                    );
                }
                existing.setIsDefault(true);
                translationAccountRepository.setOnlyOneDefault(id);
                log.info("Account {} set as default, all others unset", id);
            } else if (updateDTO.getIsDefault() != null) {
                existing.setIsDefault(false);
            }

            TranslationAccount updated = translationAccountRepository.save(existing);
            log.info("Translation account updated: {}", id);

            providerFactory.invalidateCache();

            TranslationAccountDTO dto = translationAccountGetService.convertToDTO(updated);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Translation account updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating translation account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update translation account", "TRANSLATION_ACCOUNT_UPDATE_FAILED")
            );
        }
    }
}
