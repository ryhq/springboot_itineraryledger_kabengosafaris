package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailMessageGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * §2 — Thread access. The frontend's preferred shape: request a thread
 * directly by its RFC-2822 derived threadId without first opening a
 * message. Returns thread metadata + all messages oldest → newest.
 */
@RestController
@RequestMapping("/api/email-accounts/{accountId}/threads")
@RequiredArgsConstructor
public class EmailThreadController {

    private final EmailMessageGetService emailMessageGetService;

    @GetMapping("/{threadId}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getThread(
            @PathVariable("accountId") String accountId,
            @PathVariable("threadId") String threadId) {
        return emailMessageGetService.getThreadById(accountId, threadId);
    }
}
