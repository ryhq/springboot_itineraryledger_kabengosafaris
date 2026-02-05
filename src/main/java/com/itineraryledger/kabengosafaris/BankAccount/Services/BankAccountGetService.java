package com.itineraryledger.kabengosafaris.BankAccount.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.BankAccount.DTOs.BankAccountDTO;
import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.BankAccount.Specifications.BankAccountSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * BankAccountGetService - Service for retrieving and filtering bank accounts with pagination
 *
 * This service provides methods to fetch bank accounts with:
 * - Specification-based dynamic filtering
 * - Pagination and sorting support
 * - Obfuscated ID conversion for DTOs
 */
@Service
@Slf4j
public class BankAccountGetService {

    private final BankAccountRepository bankAccountRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public BankAccountGetService(BankAccountRepository bankAccountRepository, IdObfuscator idObfuscator) {
        this.bankAccountRepository = bankAccountRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all bank accounts with pagination, filtering, and sorting
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param currency Filter by currency code (exact match)
     * @param isActive Filter by active status
     * @param isDefault Filter by default status
     * @param search Search keyword (account name, bank name, account number, account code)
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getAllBankAccounts(
        int page,
        int size,
        String currency,
        Boolean isActive,
        Boolean isDefault,
        String search,
        String sortDir
    ) {

        log.debug("Fetching bank accounts with filters - page: {}, size: {}, currency: {}, isActive: {}, " +
                "isDefault: {}, search: {}, sortDir: {}",
                page, size, currency, isActive, isDefault, search, sortDir);

        // Validate pagination parameters
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            log.warn("Invalid page size: {}", size);
            return ResponseEntity.badRequest().body("Page size must be greater than 0");
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, "createdAt")
        );

        // Build dynamic specification
        Specification<BankAccount> specification = Specification.unrestricted();

        if (currency != null && !currency.isBlank()) {
            specification = specification.and(BankAccountSpecification.byCurrency(currency));
        }

        if (isActive != null) {
            specification = specification.and(BankAccountSpecification.byIsActive(isActive));
        }

        if (isDefault != null) {
            specification = specification.and(BankAccountSpecification.byIsDefault(isDefault));
        }

        if (search != null && !search.isBlank()) {
            specification = specification.and(BankAccountSpecification.searchByKeyword(search));
        }

        // Execute query with specifications
        Page<BankAccount> pagedBankAccounts = bankAccountRepository.findAll(specification, paging);

        // Convert to DTOs
        List<BankAccountDTO> bankAccountDTOs = getBankAccountDTOs(pagedBankAccounts.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("bankAccounts", bankAccountDTOs);
        response.put("currentPage", pagedBankAccounts.getNumber());
        response.put("totalItems", pagedBankAccounts.getTotalElements());
        response.put("totalPages", pagedBankAccounts.getTotalPages());

        log.info("Successfully fetched {} bank accounts on page {}", bankAccountDTOs.size(), page);
        return ResponseEntity.ok(
            ApiResponse.success(
                200,
                "Successfully retrieved bank accounts.",
                response
            )
        );
    }

    /**
     * Get single bank account by obfuscated ID
     *
     * @param idObfuscated The obfuscated bank account ID
     * @return ResponseEntity with bank account or error
     */
    public ResponseEntity<ApiResponse<?>> getBankAccount(String idObfuscated) {
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);

            BankAccount bankAccount = bankAccountRepository.findById(id).orElse(null);

            if (bankAccount == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(
                                404,
                                "Bank account not found",
                                "BANK_ACCOUNT_NOT_FOUND"
                        )
                );
            }

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved bank account.",
                    convertToDTO(bankAccount)
                )
            );

        } catch (Exception e) {
            log.error("Error getting bank account: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            500,
                            "Failed to get bank account",
                            "GET_BANK_ACCOUNT_FAILED"
                    )
            );
        }
    }

    /**
     * Convert list of BankAccount entities to BankAccountDTOs with obfuscated IDs
     *
     * @param bankAccounts The entities to convert
     * @return List of BankAccountDTO with obfuscated IDs
     */
    private List<BankAccountDTO> getBankAccountDTOs(List<BankAccount> bankAccounts) {
        return bankAccounts.stream().map(this::convertToDTO).toList();
    }

    /**
     * Convert BankAccount entity to BankAccountDTO with obfuscated ID
     *
     * @param bankAccount The entity to convert
     * @return BankAccountDTO with obfuscated ID
     */
    public BankAccountDTO convertToDTO(BankAccount bankAccount) {
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
}
