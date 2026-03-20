package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Public webhook endpoint for Resend event callbacks.
 * No authentication required — Resend calls this endpoint directly.
 * Security is handled via svix signature verification in ResendWebhookService.
 */
@RestController
@RequestMapping("/api/public/webhooks")
@Slf4j
public class ResendWebhookController {

    private final ResendWebhookService resendWebhookService;

    @Autowired
    public ResendWebhookController(ResendWebhookService resendWebhookService) {
        this.resendWebhookService = resendWebhookService;
    }

    /**
     * Receive Resend webhook events.
     *
     * Resend sends POST requests with:
     * - Body: JSON payload with event type and data
     * - Headers: svix-id, svix-timestamp, svix-signature for verification
     */
    @PostMapping("/resend")
    public ResponseEntity<Void> handleResendWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "svix-id", required = false) String svixId,
            @RequestHeader(value = "svix-timestamp", required = false) String svixTimestamp,
            @RequestHeader(value = "svix-signature", required = false) String svixSignature) {

        log.debug("Received Resend webhook: svixId={}", svixId);

        if (svixId == null || svixId.isBlank()) {
            log.warn("Resend webhook received without svix-id header");
            return ResponseEntity.badRequest().build();
        }

        boolean processed = resendWebhookService.processWebhook(payload, svixId, svixTimestamp, svixSignature);

        if (processed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
