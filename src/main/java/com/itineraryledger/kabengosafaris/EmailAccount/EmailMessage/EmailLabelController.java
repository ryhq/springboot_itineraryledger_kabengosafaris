package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.ApplyLabelsDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.BulkApplyLabelsDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateEmailLabelDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.UpdateEmailLabelDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailLabelService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-accounts/{accountId}")
@Validated
@RequiredArgsConstructor
public class EmailLabelController {

    private final EmailLabelService emailLabelService;

    // ─── Label CRUD ───────────────────────────────────────────────────

    @GetMapping("/labels")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getLabels(@PathVariable("accountId") String accountId) {
        return emailLabelService.getLabels(accountId);
    }

    @PostMapping("/labels")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> createLabel(
            @PathVariable("accountId") String accountId,
            @RequestBody @Validated CreateEmailLabelDTO dto) {
        return emailLabelService.createLabel(accountId, dto);
    }

    @PutMapping("/labels/{labelId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> updateLabel(
            @PathVariable("accountId") String accountId,
            @PathVariable("labelId") String labelId,
            @RequestBody @Validated UpdateEmailLabelDTO dto) {
        return emailLabelService.updateLabel(accountId, labelId, dto);
    }

    @DeleteMapping("/labels/{labelId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> deleteLabel(
            @PathVariable("accountId") String accountId,
            @PathVariable("labelId") String labelId) {
        return emailLabelService.deleteLabel(accountId, labelId);
    }

    // ─── Apply labels to messages ─────────────────────────────────────

    @PutMapping("/messages/{id}/labels")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> applyLabels(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id,
            @RequestBody ApplyLabelsDTO dto) {
        return emailLabelService.applyToMessage(accountId, id, dto);
    }

    @PutMapping("/messages/batch/labels")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> bulkApplyLabels(
            @PathVariable("accountId") String accountId,
            @RequestBody BulkApplyLabelsDTO dto) {
        return emailLabelService.bulkApply(accountId, dto);
    }
}
