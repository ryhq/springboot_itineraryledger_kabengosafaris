package com.itineraryledger.kabengosafaris.Translation.Account.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Providers.TranslationProviderFactory;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class TranslationAccountDeleteService {

    private final TranslationAccountRepository translationAccountRepository;
    private final TranslationProviderFactory providerFactory;
    private final IdObfuscator idObfuscator;

    @Autowired
    public TranslationAccountDeleteService(
            TranslationAccountRepository translationAccountRepository,
            TranslationProviderFactory providerFactory,
            IdObfuscator idObfuscator) {
        this.translationAccountRepository = translationAccountRepository;
        this.providerFactory = providerFactory;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> deleteTranslationAccounts(List<String> idObfuscatedList) {
        log.info("Deleting {} translation accounts", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    ids.add(idObfuscator.decodeId(idObfuscated));
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated);
                }
            }

            // Validate no default accounts
            List<Long> defaultAccountIds = new ArrayList<>();
            for (Long id : ids) {
                TranslationAccount account = translationAccountRepository.findById(id).orElse(null);
                if (account != null && Boolean.TRUE.equals(account.getIsDefault())) {
                    defaultAccountIds.add(id);
                }
            }

            if (!defaultAccountIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot delete accounts: " + defaultAccountIds.size() + " account(s) are set as default. Change the default account first.",
                        "CANNOT_DELETE_DEFAULT_ACCOUNTS")
                );
            }

            for (Long id : ids) {
                try {
                    if (!translationAccountRepository.existsById(id)) {
                        log.warn("Translation account not found: {}", id);
                        continue;
                    }
                    ((TranslationAccountDeleteService) AopContext.currentProxy()).deleteTranslationAccount(id);
                    log.info("Translation account deleted: {}", id);
                } catch (Exception e) {
                    log.error("Error deleting translation account: {}", id, e);
                }
            }

            providerFactory.invalidateCache();

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Translation accounts deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting translation accounts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete translation accounts", "TRANSLATION_ACCOUNTS_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_TRANSLATION_ACCOUNT", description = "Deleting translation account", entityType = "TranslationAccount", entityIdParamName = "id")
    public void deleteTranslationAccount(Long id) {
        translationAccountRepository.deleteById(id);
    }
}
