package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailSyncService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-accounts/{accountId}/sync")
@RequiredArgsConstructor
public class EmailSyncController {

    private final EmailSyncService emailSyncService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> triggerSync(@PathVariable("accountId") String accountId) {
        return emailSyncService.triggerSyncApi(accountId);
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getSyncStatus(@PathVariable("accountId") String accountId) {
        return emailSyncService.getSyncStatus(accountId);
    }
}
