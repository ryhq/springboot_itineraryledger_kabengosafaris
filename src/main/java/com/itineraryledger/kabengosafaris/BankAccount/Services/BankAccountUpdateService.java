package com.itineraryledger.kabengosafaris.BankAccount.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BankAccount.DTOs.BankAccountDTO;
import com.itineraryledger.kabengosafaris.BankAccount.DTOs.UpdateBankAccountDTO;
import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for updating bank accounts
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BankAccountUpdateService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final BankAccountGetService bankAccountGetService;

    @AuditLogAnnotation(
        action = "UPDATE_BANK_ACCOUNT",
        description = "Updating a bank account",
        entityType = "BankAccount",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateBankAccount(String idObfuscated, UpdateBankAccountDTO updateDTO) {
        log.info("Updating bank account with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode bank account ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid bank account ID", "INVALID_BANK_ACCOUNT_ID")
                );
            }

            // Find bank account
            BankAccount bankAccount = bankAccountRepository.findById(id).orElse(null);
            if (bankAccount == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Bank account not found", "BANK_ACCOUNT_NOT_FOUND")
                );
            }

            // Update account name
            if (updateDTO.getAccountName() != null) {
                bankAccount.setAccountName(updateDTO.getAccountName());
            }

            // Update description
            if (updateDTO.getDescription() != null) {
                bankAccount.setDescription(updateDTO.getDescription());
            }

            // Update bank details
            if (updateDTO.getBankName() != null) {
                bankAccount.setBankName(updateDTO.getBankName());
            }
            if (updateDTO.getBankBranch() != null) {
                bankAccount.setBankBranch(updateDTO.getBankBranch());
            }
            if (updateDTO.getBranchAddress() != null) {
                bankAccount.setBranchAddress(updateDTO.getBranchAddress());
            }
            if (updateDTO.getBranchCity() != null) {
                bankAccount.setBranchCity(updateDTO.getBranchCity());
            }
            if (updateDTO.getBranchCountry() != null) {
                bankAccount.setBranchCountry(updateDTO.getBranchCountry());
            }

            // Update account details
            if (updateDTO.getAccountNumber() != null) {
                bankAccount.setAccountNumber(updateDTO.getAccountNumber());
            }
            if (updateDTO.getAccountHolderName() != null) {
                bankAccount.setAccountHolderName(updateDTO.getAccountHolderName());
            }

            // Update currency (validate format)
            if (updateDTO.getCurrency() != null) {
                String currency = updateDTO.getCurrency().trim().toUpperCase();
                if (!currency.matches("^[A-Z]{3}$")) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Currency must be a 3-letter ISO code", "INVALID_CURRENCY_FORMAT")
                    );
                }
                bankAccount.setCurrency(currency);
            }

            // Update international codes
            if (updateDTO.getSwiftBicCode() != null) {
                bankAccount.setSwiftBicCode(updateDTO.getSwiftBicCode());
            }
            if (updateDTO.getIban() != null) {
                bankAccount.setIban(updateDTO.getIban());
            }
            if (updateDTO.getRoutingNumber() != null) {
                bankAccount.setRoutingNumber(updateDTO.getRoutingNumber());
            }
            if (updateDTO.getSortCode() != null) {
                bankAccount.setSortCode(updateDTO.getSortCode());
            }
            if (updateDTO.getIntermediaryBankName() != null) {
                bankAccount.setIntermediaryBankName(updateDTO.getIntermediaryBankName());
            }
            if (updateDTO.getIntermediarySwiftCode() != null) {
                bankAccount.setIntermediarySwiftCode(updateDTO.getIntermediarySwiftCode());
            }

            // Update isDefault (handle unsetting other defaults)
            if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault()) {
                List<BankAccount> existingDefaults = bankAccountRepository
                    .findByCurrencyAndIsDefault(bankAccount.getCurrency(), true);
                for (BankAccount existingDefault : existingDefaults) {
                    if (!existingDefault.getId().equals(id)) {
                        existingDefault.setIsDefault(false);
                        bankAccountRepository.save(existingDefault);
                    }
                }
                bankAccount.setIsDefault(true);
                log.info("Set as default bank account for currency {}", bankAccount.getCurrency());
            } else if (updateDTO.getIsDefault() != null && !updateDTO.getIsDefault()) {
                bankAccount.setIsDefault(false);
            }

            // Update isActive
            if (updateDTO.getIsActive() != null) {
                bankAccount.setIsActive(updateDTO.getIsActive());
            }

            // Update notes
            if (updateDTO.getInternalNotes() != null) {
                bankAccount.setInternalNotes(updateDTO.getInternalNotes());
            }
            if (updateDTO.getInvoiceDisplayNotes() != null) {
                bankAccount.setInvoiceDisplayNotes(updateDTO.getInvoiceDisplayNotes());
            }

            // Set updated by current user
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                bankAccount.setUpdatedBy(currentUser);
            }

            // Save updated bank account
            bankAccount = bankAccountRepository.save(bankAccount);

            log.info("Bank account updated successfully: {}", bankAccount.getAccountCode());

            // Convert to DTO
            BankAccountDTO bankAccountDTO = bankAccountGetService.convertToDTO(bankAccount);

            return ResponseEntity.ok(
                ApiResponse.success(200, "Bank account updated successfully", bankAccountDTO)
            );

        } catch (Exception e) {
            log.error("Error updating bank account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update bank account", "BANK_ACCOUNT_UPDATE_FAILED")
            );
        }
    }

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
