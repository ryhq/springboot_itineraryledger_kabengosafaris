package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreatePinnedContactDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.PinnedContactService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-accounts/{accountId}/pinned-contacts")
@Validated
@RequiredArgsConstructor
public class PinnedContactController {

    private final PinnedContactService pinnedContactService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable("accountId") String accountId) {
        return pinnedContactService.list(accountId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> create(
            @PathVariable("accountId") String accountId,
            @RequestBody @Validated CreatePinnedContactDTO dto) {
        return pinnedContactService.create(accountId, dto);
    }

    @DeleteMapping("/{pinnedId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> delete(
            @PathVariable("accountId") String accountId,
            @PathVariable("pinnedId") String pinnedId) {
        return pinnedContactService.delete(accountId, pinnedId);
    }
}
