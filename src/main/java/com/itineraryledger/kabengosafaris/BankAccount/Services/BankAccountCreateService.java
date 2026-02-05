package com.itineraryledger.kabengosafaris.BankAccount.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BankAccount.DTOs.BankAccountDTO;
import com.itineraryledger.kabengosafaris.BankAccount.DTOs.CreateBankAccountDTO;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BankAccountCreateService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "CREATE_BANK_ACCOUNT",
        description = "Creating a new bank account",
        entityType = "BankAccount"
    )
    public ResponseEntity<ApiResponse<?>> createBankAccount(CreateBankAccountDTO createDTO) {
        log.info("Creating new bank account for currency: {}", createDTO.getCurrency());

        try {
            // Validate currency format
            String currency = createDTO.getCurrency().trim().toUpperCase();
            if (!currency.matches("^[A-Z]{3}$")) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Currency must be a 3-letter ISO code", "INVALID_CURRENCY_FORMAT")
                );
            }

            // Handle isDefault logic: if setting as default, unset other defaults for this currency
            Boolean isDefault = createDTO.getIsDefault() != null ? createDTO.getIsDefault() : false;
            if (isDefault) {
                List<BankAccount> existingDefaults = bankAccountRepository.findByCurrencyAndIsDefault(currency, true);
                for (BankAccount existingDefault : existingDefaults) {
                    existingDefault.setIsDefault(false);
                    bankAccountRepository.save(existingDefault);
                }
                log.info("Unset {} existing default bank account(s) for currency {}", existingDefaults.size(), currency);
            }

            // Get current user for audit tracking
            User currentUser = getCurrentUser();

            // Build bank account entity
            BankAccount bankAccount = BankAccount.builder()
                .accountCode("TEMP") // Temporary code, will be updated after save
                .accountName(createDTO.getAccountName())
                .description(createDTO.getDescription())
                .bankName(createDTO.getBankName())
                .bankBranch(createDTO.getBankBranch())
                .branchAddress(createDTO.getBranchAddress())
                .branchCity(createDTO.getBranchCity())
                .branchCountry(createDTO.getBranchCountry())
                .accountNumber(createDTO.getAccountNumber())
                .accountHolderName(createDTO.getAccountHolderName())
                .currency(currency)
                .swiftBicCode(createDTO.getSwiftBicCode())
                .iban(createDTO.getIban())
                .routingNumber(createDTO.getRoutingNumber())
                .sortCode(createDTO.getSortCode())
                .intermediaryBankName(createDTO.getIntermediaryBankName())
                .intermediarySwiftCode(createDTO.getIntermediarySwiftCode())
                .isDefault(isDefault)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .internalNotes(createDTO.getInternalNotes())
                .invoiceDisplayNotes(createDTO.getInvoiceDisplayNotes())
                .createdBy(currentUser)
                .build();

            // Save to get ID
            bankAccount = bankAccountRepository.save(bankAccount);

            // Generate account code based on ID
            String accountCode = bankAccount.generateCode();
            bankAccount.setAccountCode(accountCode);

            // Save again with the generated code
            bankAccount = bankAccountRepository.save(bankAccount);

            log.info("Bank account created successfully with code: {}", accountCode);

            // Convert to DTO
            BankAccountDTO bankAccountDTO = convertToDTO(bankAccount);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Bank account created successfully", bankAccountDTO)
            );

        } catch (Exception e) {
            log.error("Error creating bank account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create bank account", "BANK_ACCOUNT_CREATE_FAILED")
            );
        }
    }

    /**
     * Convert BankAccount entity to BankAccountDTO
     */
    private BankAccountDTO convertToDTO(BankAccount bankAccount) {
        BankAccountDTO dto = BankAccountDTO.builder()
            .id(idObfuscator.encodeId(bankAccount.getId()))
            .accountCode(bankAccount.getAccountCode())
            .accountName(bankAccount.getAccountName())
            .description(bankAccount.getDescription())
            .bankName(bankAccount.getBankName())
            .bankBranch(bankAccount.getBankBranch())
            .branchAddress(bankAccount.getBranchAddress())
            .branchCity(bankAccount.getBranchCity())
            .branchCountry(bankAccount.getBranchCountry())
            .accountNumber(bankAccount.getAccountNumber())
            .accountHolderName(bankAccount.getAccountHolderName())
            .currency(bankAccount.getCurrency())
            .swiftBicCode(bankAccount.getSwiftBicCode())
            .iban(bankAccount.getIban())
            .routingNumber(bankAccount.getRoutingNumber())
            .sortCode(bankAccount.getSortCode())
            .intermediaryBankName(bankAccount.getIntermediaryBankName())
            .intermediarySwiftCode(bankAccount.getIntermediarySwiftCode())
            .isDefault(bankAccount.getIsDefault())
            .isActive(bankAccount.getIsActive())
            .internalNotes(bankAccount.getInternalNotes())
            .invoiceDisplayNotes(bankAccount.getInvoiceDisplayNotes())
            .createdAt(bankAccount.getCreatedAt())
            .updatedAt(bankAccount.getUpdatedAt())
            .build();

        // Set created by if present
        if (bankAccount.getCreatedBy() != null) {
            dto.setCreatedByName(bankAccount.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (bankAccount.getUpdatedBy() != null) {
            dto.setUpdatedByName(bankAccount.getUpdatedBy().getUsername());
        }

        return dto;
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
                // Fetch from repository to ensure it's a managed entity
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
