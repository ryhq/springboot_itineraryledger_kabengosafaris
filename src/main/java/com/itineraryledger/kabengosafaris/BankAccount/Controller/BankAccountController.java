package com.itineraryledger.kabengosafaris.BankAccount.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.BankAccount.DTOs.CreateBankAccountDTO;
import com.itineraryledger.kabengosafaris.BankAccount.Specifications.BankAccountFilter;
import com.itineraryledger.kabengosafaris.BankAccount.DTOs.UpdateBankAccountDTO;
import com.itineraryledger.kabengosafaris.BankAccount.Services.BankAccountCreateService;
import com.itineraryledger.kabengosafaris.BankAccount.Services.BankAccountDeleteService;
import com.itineraryledger.kabengosafaris.BankAccount.Services.BankAccountGetService;
import com.itineraryledger.kabengosafaris.BankAccount.Services.BankAccountUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/bank-accounts")
@Slf4j
public class BankAccountController {

    private final BankAccountCreateService bankAccountCreateService;
    private final BankAccountUpdateService bankAccountUpdateService;
    private final BankAccountDeleteService bankAccountDeleteService;
    private final BankAccountGetService bankAccountGetService;

    @Autowired
    public BankAccountController(
        BankAccountCreateService bankAccountCreateService,
        BankAccountUpdateService bankAccountUpdateService,
        BankAccountDeleteService bankAccountDeleteService,
        BankAccountGetService bankAccountGetService
    ) {
        this.bankAccountCreateService = bankAccountCreateService;
        this.bankAccountUpdateService = bankAccountUpdateService;
        this.bankAccountDeleteService = bankAccountDeleteService;
        this.bankAccountGetService = bankAccountGetService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_BANK_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> createBankAccount(
        @Valid @RequestBody CreateBankAccountDTO createBankAccountDTO
    ) {
        log.info("POST /api/bank-accounts - Creating new bank account: {}", createBankAccountDTO.getAccountName());
        return bankAccountCreateService.createBankAccount(createBankAccountDTO);
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BANK_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> updateBankAccount(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateBankAccountDTO updateBankAccountDTO
    ) {
        log.info("PUT /api/bank-accounts/{} - Updating bank account", idObfuscated);
        return bankAccountUpdateService.updateBankAccount(idObfuscated, updateBankAccountDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_BANK_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> deleteBankAccounts(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/bank-accounts - Deleting {} bank account(s)", idObfuscatedList.size());
        return bankAccountDeleteService.deleteBankAccounts(idObfuscatedList);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_BANK_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> getBankAccountById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/bank-accounts/{} - Fetching bank account by ID", idObfuscated);
        return bankAccountGetService.getBankAccount(idObfuscated);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_BANK_ACCOUNT')")
    public ResponseEntity<?> getAllBankAccounts(
        @ModelAttribute BankAccountFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/bank-accounts - Fetching all bank accounts with filters");
        return bankAccountGetService.getAllBankAccounts(
            filter, includeStats, page, size, sortBy, sortDirection);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Retiring several accounts at once is the ordinary case when a bank is
     * changed; doing it one at a time leaves the set half-changed with nobody the
     * wiser. Reports per-id outcomes rather than a bare 200.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BANK_ACCOUNT')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("bank account", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
