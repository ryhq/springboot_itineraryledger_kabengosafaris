package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.CreateEmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.UpdateEmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureCreateService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureDeleteService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureFilter;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureListService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.BulkFlags;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Signatures as a resource of their own, at /api/email-signatures.
 *
 * The nested surface under an account stays exactly as it was — nothing that calls it needs
 * changing — but a signature is something somebody writes, reviews and searches across
 * mailboxes, and a resource reachable only by first knowing its parent cannot have a list
 * page, a record page or a filter of its own.
 *
 * Every write resolves the signature's account and hands off to the same nested service, so
 * there is one implementation of each rule rather than two that drift — including the guard
 * that a shipped signature can be restored but never deleted.
 *
 * The permission names are the nested controller's, exactly: a permission is matched by
 * NAME, and inventing PERM_READ_EMAIL_SIGNATURE here would have 403'd everybody — SUPERADMIN
 * included — for a resource that already has perfectly good permissions.
 */
@RestController
@RequestMapping("/api/email-signatures")
@RequiredArgsConstructor
@Slf4j
public class EmailSignatureFlatController {

    private final EmailAccountSignatureListService listService;
    private final EmailAccountSignatureCreateService createService;
    private final EmailAccountSignatureUpdateService updateService;
    private final EmailAccountSignatureDeleteService deleteService;
    private final EmailAccountSignatureRepository signatureRepository;
    private final BulkFlags bulkFlags;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> list(
        @ModelAttribute EmailAccountSignatureFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return listService.list(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /** The record, WITH its body, plus where it sits in the caller's filtered set. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> getOne(
        @PathVariable String id,
        @ModelAttribute EmailAccountSignatureFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return listService.getOne(id, filter, sortBy, sortDirection);
    }

    /**
     * Creating one needs to know which mailbox it signs for, so `emailAccountId` is required
     * — there is no such thing as a signature belonging to nothing.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> create(@RequestBody CreateEmailAccountSignatureDTO body) {
        if (body == null || body.getEmailAccountId() == null || body.getEmailAccountId().isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Which mailbox is this signature for? emailAccountId is required",
                    "EMAIL_ACCOUNT_ID_REQUIRED"));
        }
        return createService.createSignature(body.getEmailAccountId(), body);
    }

    /**
     * Enabling or disabling a selection in one request.
     *
     * Only `enabled` is offered. Which signature an account uses is a choice between
     * siblings — turning several on at once cannot express it, and a bulk control that
     * silently picked a winner would be worse than not having one.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<?> bulkUpdate(@RequestBody BulkFlags.Request request) {
        return bulkFlags.apply("signature", signatureRepository, request, signature -> {
            if (request.getIsActive() != null) signature.setEnabled(request.getIsActive());
        });
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> update(
        @PathVariable String id,
        @RequestBody UpdateEmailAccountSignatureDTO body
    ) {
        String accountId = listService.accountIdOf(id);
        if (accountId == null) return notFound();
        return updateService.updateSignature(accountId, id, body);
    }

    /** Puts a shipped signature back to the version that came with the system. */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> restore(@PathVariable String id) {
        String accountId = listService.accountIdOf(id);
        if (accountId == null) return notFound();
        return updateService.restoreSystemDefaultSignature(accountId, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> deleteOne(@PathVariable String id) {
        String accountId = listService.accountIdOf(id);
        if (accountId == null) return notFound();
        return deleteService.deleteSignatures(accountId, List.of(id));
    }

    /**
     * Bare array body, as everywhere else in this API.
     *
     * The ids may span mailboxes, so they are grouped by account and each group handed to
     * the nested service that owns its rules.
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> deleteMany(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No signatures supplied", "NO_IDS"));
        }
        java.util.Map<String, List<String>> byAccount = new java.util.LinkedHashMap<>();
        for (String id : ids) {
            String accountId = listService.accountIdOf(id);
            if (accountId == null) continue;
            byAccount.computeIfAbsent(accountId, key -> new java.util.ArrayList<>()).add(id);
        }
        if (byAccount.isEmpty()) return notFound();

        int deletedCount = 0;
        List<String> deletedIds = new java.util.ArrayList<>();
        List<java.util.Map<String, Object>> skipped = new java.util.ArrayList<>();

        for (var entry : byAccount.entrySet()) {
            ResponseEntity<ApiResponse<?>> result =
                deleteService.deleteSignatures(entry.getKey(), entry.getValue());
            Object data = result.getBody() != null ? result.getBody().getData() : null;
            if (result.getStatusCode().is2xxSuccessful() && data instanceof java.util.Map<?, ?> report) {
                Object count = report.get("deletedCount");
                if (count instanceof Number number) deletedCount += number.intValue();
                Object ok = report.get("deletedIds");
                if (ok instanceof List<?> list) list.forEach(v -> deletedIds.add(String.valueOf(v)));
                Object bad = report.get("skipped");
                if (bad instanceof List<?> list) {
                    for (Object row : list) {
                        if (row instanceof java.util.Map<?, ?> map) {
                            java.util.Map<String, Object> copy = new java.util.HashMap<>();
                            map.forEach((k, v) -> copy.put(String.valueOf(k), v));
                            skipped.add(copy);
                        }
                    }
                }
            } else {
                // a whole group refused: say so per id rather than losing it
                String reason = result.getBody() != null ? result.getBody().getMessage() : "Could not be deleted";
                for (String id : entry.getValue()) {
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    row.put("id", id);
                    row.put("reason", reason);
                    skipped.add(row);
                }
            }
        }

        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("deletedCount", deletedCount);
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);
        return ResponseEntity.ok(ApiResponse.success(200,
            deletedCount + " signature" + (deletedCount == 1 ? "" : "s") + " deleted", report));
    }

    private ResponseEntity<ApiResponse<?>> notFound() {
        return ResponseEntity.status(404).body(
            ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND"));
    }

    /**
     * Restore every system-default email signatures at once.
     *
     * The one action that makes a shipped template fix real: seeded files are never overwritten, so
     * without this a correction sits in the release and nothing changes.
     */
    @PostMapping("/restore-defaults")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<?> restoreAllSystemDefaults() {
        log.info("POST /api/email-signatures/restore-defaults - restoring every system default");
        return updateService.restoreAllSystemDefaults();
    }
}
