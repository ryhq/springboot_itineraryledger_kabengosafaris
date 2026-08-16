package com.itineraryledger.kabengosafaris.Translation.Account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.CreateTranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.UpdateTranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;
import com.itineraryledger.kabengosafaris.Translation.Account.Services.TranslationAccountCreateService;
import com.itineraryledger.kabengosafaris.Translation.Account.Services.TranslationAccountDeleteService;
import com.itineraryledger.kabengosafaris.Translation.Account.Services.TranslationAccountFilter;
import com.itineraryledger.kabengosafaris.Translation.Account.Services.TranslationAccountGetService;
import com.itineraryledger.kabengosafaris.Translation.Account.Services.TranslationAccountTestService;
import com.itineraryledger.kabengosafaris.Translation.Account.Services.TranslationAccountUpdateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/translation-accounts")
public class TranslationAccountController {

    @Autowired
    private TranslationAccountGetService translationAccountGetService;

    @Autowired
    private TranslationAccountCreateService translationAccountCreateService;

    @Autowired
    private TranslationAccountUpdateService translationAccountUpdateService;

    @Autowired
    private TranslationAccountTestService translationAccountTestService;

    @Autowired
    private TranslationAccountDeleteService translationAccountDeleteService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_TRANSLATION_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> createTranslationAccount(
        @Valid @RequestBody CreateTranslationAccountDTO createDTO
    ) {
        return translationAccountCreateService.createTranslationAccount(createDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_TRANSLATION_ACCOUNT')")
    public ResponseEntity<?> getAllTranslationAccounts(
        /*
         * Every parameter the old signature took is still spelled the same on the wire —
         * @ModelAttribute binds them onto the filter — plus the multi-value forms
         * (providerTypes, statuses, qualities) and a keyword.
         */
        @ModelAttribute TranslationAccountFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return translationAccountGetService.getAllTranslationAccounts(
            filter, includeStats, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_TRANSLATION_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> getTranslationAccount(
        @PathVariable String id,
        // the list's filter and sort, so prev/next stays inside the set on screen
        @ModelAttribute TranslationAccountFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return translationAccountGetService.getTranslationAccount(id, filter, sortBy, sortDirection);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> updateTranslationAccount(
        @PathVariable String id,
        @Valid @RequestBody UpdateTranslationAccountDTO updateDTO
    ) {
        return translationAccountUpdateService.updateTranslationAccount(id, updateDTO);
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> testTranslationAccount(@PathVariable String id) {
        return translationAccountTestService.testTranslationAccount(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_TRANSLATION_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> deleteTranslationAccount(@PathVariable String id) {
        List<String> idList = new ArrayList<>();
        idList.add(id);
        return translationAccountDeleteService.deleteTranslationAccounts(idList);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_TRANSLATION_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> deleteTranslationAccountsBatch(@RequestBody List<String> idList) {
        return translationAccountDeleteService.deleteTranslationAccounts(idList);
    }

    @GetMapping("/providers")
    @PreAuthorize("hasAuthority('PERM_READ_TRANSLATION_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> getProviderTypes() {
        List<Map<String, Object>> providers = new ArrayList<>();
        for (TranslationProviderType type : TranslationProviderType.values()) {
            Map<String, Object> provider = new HashMap<>();
            provider.put("id", type.toInteger());
            provider.put("name", type.name());
            provider.put("displayName", type.getDisplayName());
            providers.add(provider);
        }
        return ResponseEntity.ok(
            ApiResponse.success(200, "Available translation provider types", providers)
        );
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — switch a whole selection off at once.
     *
     * DISABLING only, and that is not an oversight. An account is enabled by passing a test
     * (TranslationAccountTestService sets the flag on success) precisely so that nothing can
     * be marked usable without something having proved it works. A bulk "enable" would route
     * around the one check that makes the flag mean anything, so a request to enable is
     * reported back as skipped with the reason rather than silently obeyed or silently
     * dropped.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TRANSLATION_ACCOUNT')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        if (Boolean.TRUE.equals(request.getIsActive())) {
            return ResponseEntity.badRequest().body(
                com.itineraryledger.kabengosafaris.Response.ApiResponse.error(400,
                    "An account is enabled by testing it, not by switching it on. Run the connection test instead.",
                    "ENABLE_REQUIRES_TEST")
            );
        }
        return bulkFlags.apply("translation account", bulkFlagsRepository, request, entity -> {
            if (Boolean.FALSE.equals(request.getIsActive())) {
                entity.setEnabled(false);
                // a disabled account cannot remain the one everything routes through
                entity.setIsDefault(false);
            }
        });
    }
}
