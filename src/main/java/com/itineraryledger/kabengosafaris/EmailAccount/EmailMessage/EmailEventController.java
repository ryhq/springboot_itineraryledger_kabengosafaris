package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailEventBus;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;

/**
 * §9 — server-sent events for inbox-realtime UI updates.
 * Event names: ready · message.new · message.updated · sync.completed.
 *
 * Explicit bean name to avoid collision with
 * com.itineraryledger.kabengosafaris.EmailEvent.EmailEventController
 * (both classes would otherwise auto-derive bean name "emailEventController").
 */
@RestController("emailMessageEventController")
@RequestMapping("/api/email-accounts/{accountId}/events")
@RequiredArgsConstructor
public class EmailEventController {

    private final EmailEventBus eventBus;
    private final IdObfuscator idObfuscator;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public SseEmitter subscribe(@PathVariable("accountId") String accountId) {
        Long id = idObfuscator.decodeId(accountId);
        return eventBus.subscribe(id);
    }
}
