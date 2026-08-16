package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authenticated controller for viewing Resend webhook events in the management app.
 */
@RestController
@RequestMapping("/api/resend-webhook-events")
@Slf4j
@RequiredArgsConstructor
public class ResendWebhookEventController {

    private final ResendWebhookEventGetService getService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> listEvents(
            @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "25") Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String fromEmail,
            @RequestParam(required = false) String toEmail,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includeStats,
            /* the house names for paging, alongside the ones this module started with */
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        log.info("GET /api/resend-webhook-events - Listing webhook events");
        return getService.listEvents(
                page != null ? page : pageNumber,
                size != null ? size : pageSize,
                sortBy, sortDirection, eventType, fromEmail, toEmail, keyword, includeStats);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_ACCOUNT')")
    public ResponseEntity<ApiResponse<?>> getEvent(
            @PathVariable String id,
            // the list's filter and sort, so prev/next stays inside the set on screen
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String fromEmail,
            @RequestParam(required = false) String toEmail,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        log.info("GET /api/resend-webhook-events/{} - Fetching webhook event", id);
        return getService.getEvent(id, eventType, fromEmail, toEmail, keyword, sortBy, sortDirection);
    }
}
