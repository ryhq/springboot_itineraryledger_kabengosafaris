package com.itineraryledger.kabengosafaris.BankAccount.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * BankAccountDeleteService - Service for deleting bank accounts
 */
@Service
@Slf4j
@Transactional
public class BankAccountDeleteService {

    private final BankAccountRepository bankAccountRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public BankAccountDeleteService(
        BankAccountRepository bankAccountRepository,
        IdObfuscator idObfuscator
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete bank accounts by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated bank account IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteBankAccounts(List<String> idObfuscatedList) {
        log.info("Deleting {} bank account(s)", idObfuscatedList.size());

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

            return deleteBankAccountsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting bank accounts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete bank accounts",
                    "BANK_ACCOUNTS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete bank accounts by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteBankAccountsInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                BankAccount bankAccount = bankAccountRepository.findById(id).orElse(null);

                if (bankAccount == null) {
                    log.warn("Bank account not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((BankAccountDeleteService) AopContext.currentProxy()).deleteBankAccount(id);
                deletedCount++;
                log.info("Bank account deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting bank account: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " bank account(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(action = "DELETE_BANK_ACCOUNT", description = "Deleting bank account", entityType = "BankAccount", entityIdParamName = "id")
    public void deleteBankAccount(Long id) {
        bankAccountRepository.deleteById(id);
    }
}
