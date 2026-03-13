package com.itineraryledger.kabengosafaris.Translation.Account.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.CreateTranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.TranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProviderFactory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TranslationAccountCreateService {

    private final TranslationAccountRepository translationAccountRepository;
    private final TranslationAccountGetService translationAccountGetService;
    private final TranslationProviderFactory providerFactory;

    @Autowired
    public TranslationAccountCreateService(
            TranslationAccountRepository translationAccountRepository,
            TranslationAccountGetService translationAccountGetService,
            TranslationProviderFactory providerFactory) {
        this.translationAccountRepository = translationAccountRepository;
        this.translationAccountGetService = translationAccountGetService;
        this.providerFactory = providerFactory;
    }

    @AuditLogAnnotation(
        action = "CREATE_TRANSLATION_ACCOUNT",
        description = "Creating a new translation account",
        entityType = "TranslationAccount"
    )
    public ResponseEntity<ApiResponse<?>> createTranslationAccount(CreateTranslationAccountDTO createDTO) {
        log.info("Creating new translation account: {}", createDTO.getName());

        try {
            // Validate provider type
            TranslationProviderType providerType;
            try {
                providerType = TranslationProviderType.fromInteger(createDTO.getProviderType());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, e.getMessage(), "INVALID_PROVIDER_TYPE")
                );
            }

            // Check for duplicate name
            if (translationAccountRepository.findByName(createDTO.getName()).isPresent()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Account name already exists", "DUPLICATE_NAME")
                );
            }

            // Encrypt API key if provided
            String encryptedApiKey = null;
            if (createDTO.getApiKey() != null && !createDTO.getApiKey().isBlank()) {
                encryptedApiKey = EncryptionUtil.encrypt(createDTO.getApiKey());
            }

            // Build entity
            TranslationAccount account = TranslationAccount.builder()
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .providerType(providerType)
                .apiKey(encryptedApiKey)
                .baseUrl(createDTO.getBaseUrl())
                .timeoutSeconds(createDTO.getTimeoutSeconds())
                .build();

            TranslationAccount saved = translationAccountRepository.save(account);
            log.info("Translation account created with ID: {}", saved.getId());

            providerFactory.invalidateCache();

            TranslationAccountDTO dto = translationAccountGetService.convertToDTO(saved);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Translation account created successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error creating translation account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create translation account", "TRANSLATION_ACCOUNT_CREATE_FAILED")
            );
        }
    }
}
