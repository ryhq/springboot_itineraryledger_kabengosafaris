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

    /** One skipped row, named the way the caller can show it. */
    private java.util.Map<String, Object> skip(String id, String name, String reason) {
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", id);
        row.put("code", name);
        row.put("reason", reason);
        return row;
    }

    public ResponseEntity<ApiResponse<?>> deleteTranslationAccounts(List<String> idObfuscatedList) {
        log.info("Deleting {} translation accounts", idObfuscatedList.size());

        try {
            /*
             * Per-row outcomes. This used to refuse the WHOLE batch when any row was the
             * default account, and to answer "200, deleted successfully" whether it removed
             * everything or nothing. Skipping the default one by name and deleting the rest
             * is what somebody selecting five accounts actually meant.
             */
            int deletedCount = 0;
            List<String> deletedIds = new ArrayList<>();
            List<java.util.Map<String, Object>> skipped = new ArrayList<>();

            for (String idObfuscated : idObfuscatedList) {
                Long id;
                try {
                    id = idObfuscator.decodeId(idObfuscated);
                } catch (Exception e) {
                    skipped.add(skip(idObfuscated, null, "Not a valid account reference"));
                    continue;
                }

                TranslationAccount account = translationAccountRepository.findById(id).orElse(null);
                if (account == null) {
                    skipped.add(skip(idObfuscated, null, "No such account — it may already have been deleted"));
                    continue;
                }
                if (Boolean.TRUE.equals(account.getIsDefault())) {
                    skipped.add(skip(idObfuscated, account.getName(),
                        "This is the default account — make another one default first"));
                    continue;
                }

                try {
                    ((TranslationAccountDeleteService) AopContext.currentProxy()).deleteTranslationAccount(id);
                    deletedCount++;
                    deletedIds.add(idObfuscated);
                    log.info("Translation account deleted: {}", id);
                } catch (Exception e) {
                    log.error("Error deleting translation account: {}", id, e);
                    skipped.add(skip(idObfuscated, account.getName(),
                        e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
                }
            }

            providerFactory.invalidateCache();

            java.util.Map<String, Object> report = new java.util.HashMap<>();
            report.put("deletedCount", deletedCount);
            report.put("deletedIds", deletedIds);
            report.put("skipped", skipped);

            return ResponseEntity.ok().body(
                ApiResponse.success(200,
                    deletedCount + " translation account" + (deletedCount == 1 ? "" : "s") + " deleted",
                    report)
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
