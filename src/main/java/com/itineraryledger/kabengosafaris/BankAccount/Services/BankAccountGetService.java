package com.itineraryledger.kabengosafaris.BankAccount.Services;

import java.util.Arrays;
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
import com.itineraryledger.kabengosafaris.BankAccount.Specifications.BankAccountFilter;
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
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "accountCode", "accountName", "bankName", "currency", "isActive", "isDefault", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public BankAccountGetService(
        BankAccountRepository bankAccountRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
        this.bankAccountRepository = bankAccountRepository;
        this.idObfuscator = idObfuscator;
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<BankAccount> buildSpec(BankAccountFilter filter) {
        Specification<BankAccount> spec = Specification.<BankAccount>unrestricted()
            .and(BankAccountSpecification.byCurrencies(filter.allCurrencies()));

        if (filter.getIsActive() != null) {
            spec = spec.and(BankAccountSpecification.byIsActive(filter.getIsActive()));
        }
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            java.util.List<Boolean> states = filter.getStatuses().stream()
                .map(state -> "active".equalsIgnoreCase(state) ? Boolean.TRUE
                    : "inactive".equalsIgnoreCase(state) ? Boolean.FALSE : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
            // active AND inactive is every account: a contradiction cancels
            if (states.size() == 1) {
                spec = spec.and(BankAccountSpecification.byIsActive(states.get(0)));
            }
        }
        if (filter.getIsDefault() != null) {
            spec = spec.and(BankAccountSpecification.byIsDefault(filter.getIsDefault()));
        }

        String keyword = filter.effectiveKeyword();
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(BankAccountSpecification.searchByKeyword(keyword));
        }

        Specification<BankAccount> quality = null;
        if (filter.wants("noSwift")) quality = or(quality, BankAccountSpecification.missingSwift());
        if (filter.wants("noIban")) quality = or(quality, BankAccountSpecification.missingIban());
        if (quality != null) spec = spec.and(quality);

        return spec;
    }

    private Specification<BankAccount> or(
            Specification<BankAccount> spec, Specification<BankAccount> extra) {
        return spec == null ? extra : spec.or(extra);
    }

    /**
     * The cards that head the list.
     *
     * No balances: this app records which account money moved through, never what
     * is in one. A figure here would be a bank statement we have not seen.
     */
    private Map<String, Object> buildStats(Specification<BankAccount> spec) {
        return listStats.of(BankAccount.class, spec)
            .total()
            .count("active", BankAccountSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("noSwift", BankAccountSpecification.missingSwift())
            .count("noIban", BankAccountSpecification.missingIban())
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
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
     * @param sortBy Sort field
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getAllBankAccounts(
        BankAccountFilter filter,
        Boolean includeStats,
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) {

        log.debug("Fetching bank accounts - page: {}, size: {}, sortBy: {}", page, size, sortBy);

        // Validate sort field
        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

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
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, validatedSortBy)
        );

        Specification<BankAccount> specification =
            buildSpec(filter != null ? filter : new BankAccountFilter());

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
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        response.put("pageSize", pagedBankAccounts.getSize());
        /*
         * Counters for the WHOLE filtered set, from the same specification as the
         * rows, so a card and the table under it cannot disagree.
         */
        if (!Boolean.FALSE.equals(includeStats)) {
            response.put("stats", buildStats(specification));
        }

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
        return getBankAccountById(idObfuscated, null, null, null);
    }

    /**
     * One account, and where it sits in the set the caller was looking at.
     *
     * Paging out of a "no SWIFT" list must stay among accounts missing one.
     */
    public ResponseEntity<ApiResponse<?>> getBankAccountById(
        String idObfuscated,
        BankAccountFilter filter,
        String sortBy,
        String sortDirection
    ) {
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

            Specification<BankAccount> navSpec =
                buildSpec(filter != null ? filter : new BankAccountFilter());
            String navSortBy = validateSortField(sortBy) != null
                ? validateSortField(sortBy) : "createdAt";
            Map<String, Object> nav = recordNavigation.navigate(
                BankAccount.class, navSpec, navSortBy, "asc".equalsIgnoreCase(sortDirection), id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("bankAccount", convertToDTO(bankAccount));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved bank account.",
                    response
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
