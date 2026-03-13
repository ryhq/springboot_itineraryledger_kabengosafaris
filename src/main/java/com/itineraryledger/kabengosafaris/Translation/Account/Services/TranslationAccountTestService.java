package com.itineraryledger.kabengosafaris.Translation.Account.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.TranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProvider;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProviderFactory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class TranslationAccountTestService {

    private final TranslationAccountRepository translationAccountRepository;
    private final TranslationAccountGetService translationAccountGetService;
    private final TranslationProviderFactory providerFactory;
    private final IdObfuscator idObfuscator;

    @Autowired
    public TranslationAccountTestService(
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
        action = "TEST_TRANSLATION_ACCOUNT",
        description = "Testing translation account connection",
        entityType = "TranslationAccount",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> testTranslationAccount(String idObfuscated) {
        log.info("Testing translation account: {}", idObfuscated);

        try {
            Long id = idObfuscator.decodeId(idObfuscated);

            TranslationAccount account = translationAccountRepository.findById(id).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Translation account not found", "TRANSLATION_ACCOUNT_NOT_FOUND")
                );
            }

            TranslationProvider provider = providerFactory.createProvider(account);

            // Test 1: Check service availability
            boolean available = provider.isServiceAvailable();
            if (!available) {
                account.setLastTestedAt(LocalDateTime.now());
                account.setLastErrorMessage("Service is not available or not responding");
                translationAccountRepository.save(account);

                return ResponseEntity.ok().body(
                    ApiResponse.error(400, "Translation service is not available", "TRANSLATION_SERVICE_UNAVAILABLE")
                );
            }

            // Test 2: Try a sample translation
            try {
                String result = provider.translate("Hello", "en", "fr");
                log.info("Test translation result: Hello -> {}", result);
            } catch (Exception e) {
                account.setLastTestedAt(LocalDateTime.now());
                account.setLastErrorMessage("Translation test failed: " + e.getMessage());
                translationAccountRepository.save(account);

                TranslationAccountDTO dto = translationAccountGetService.convertToDTO(account);
                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Translation test failed: " + e.getMessage(), dto)
                );
            }

            // Success
            account.setEnabled(true);
            account.setLastTestedAt(LocalDateTime.now());
            account.setLastErrorMessage(null);
            TranslationAccount saved = translationAccountRepository.save(account);

            providerFactory.invalidateCache();

            TranslationAccountDTO dto = translationAccountGetService.convertToDTO(saved);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Translation account tested successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error testing translation account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to test translation account", "TRANSLATION_ACCOUNT_TEST_FAILED")
            );
        }
    }
}
