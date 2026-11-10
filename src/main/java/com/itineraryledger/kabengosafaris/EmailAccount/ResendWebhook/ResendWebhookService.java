package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailDeliveryStatus;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for processing Resend webhook events.
 * Handles signature verification, idempotency, event storage, and side effects.
 */
@Service
@Slf4j
public class ResendWebhookService {

    private final ResendWebhookEventRepository webhookEventRepository;
    private final ResendWebhookVerifier webhookVerifier;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ResendWebhookService(
            ResendWebhookEventRepository webhookEventRepository,
            ResendWebhookVerifier webhookVerifier,
            EmailAccountRepository emailAccountRepository,
            EmailMessageRepository emailMessageRepository,
            ObjectMapper objectMapper) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookVerifier = webhookVerifier;
        this.emailAccountRepository = emailAccountRepository;
        this.emailMessageRepository = emailMessageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Process an incoming Resend webhook event.
     *
     * @param payload       Raw JSON body
     * @param svixId        svix-id header
     * @param svixTimestamp  svix-timestamp header
     * @param svixSignature  svix-signature header
     * @return true if processed successfully, false if rejected
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public boolean processWebhook(String payload, String svixId, String svixTimestamp, String svixSignature) {
        try {
            // 1. Check idempotency first (before verification to avoid unnecessary crypto)
            if (webhookEventRepository.existsBySvixId(svixId)) {
                log.debug("Duplicate webhook event skipped: svixId={}", svixId);
                return true; // Already processed — return success
            }

            // 2. Parse the payload to extract "from" email for account lookup
            Map<String, Object> body = objectMapper.readValue(payload, Map.class);
            String eventType = (String) body.get("type");
            Map<String, Object> data = (Map<String, Object>) body.get("data");

            if (eventType == null || data == null) {
                log.warn("Invalid webhook payload: missing type or data");
                return false;
            }

            String fromEmail = (String) data.get("from");
            String emailId = (String) data.get("email_id");

            // 3. Look up the email account by "from" address to get per-account webhook secret
            EmailAccount account = findResendAccountByEmail(fromEmail);
            if (account != null && account.getWebhookSecret() != null && !account.getWebhookSecret().isBlank()) {
                // Verify signature using per-account webhook secret
                String decryptedSecret = EncryptionUtil.decrypt(account.getWebhookSecret());
                if (!webhookVerifier.verify(payload, svixId, svixTimestamp, svixSignature, decryptedSecret)) {
                    log.warn("Webhook signature verification failed for account: {}", fromEmail);
                    return false;
                }
            } else {
                // No account found or no webhook secret configured — log but still accept
                // This allows webhooks to work even before secrets are configured per-account
                log.info("No webhook secret configured for from={}, accepting without signature verification", fromEmail);
            }

            // 4. Extract event details
            String toEmail = null;
            Object toField = data.get("to");
            if (toField instanceof List) {
                List<String> toList = (List<String>) toField;
                toEmail = toList.isEmpty() ? null : toList.get(0);
            } else if (toField instanceof String) {
                toEmail = (String) toField;
            }

            String subject = (String) data.get("subject");
            String createdAtStr = (String) data.get("created_at");
            LocalDateTime eventTimestamp = null;
            if (createdAtStr != null) {
                try {
                    eventTimestamp = OffsetDateTime.parse(createdAtStr).toLocalDateTime();
                } catch (Exception e) {
                    log.debug("Could not parse event timestamp: {}", createdAtStr);
                }
            }

            // 5. Store the event
            ResendWebhookEvent event = ResendWebhookEvent.builder()
                .svixId(svixId)
                .eventType(eventType)
                .emailId(emailId)
                .fromEmail(fromEmail)
                .toEmail(toEmail)
                .subject(subject)
                .rawPayload(payload)
                .eventTimestamp(eventTimestamp)
                .build();

            webhookEventRepository.save(event);
            log.info("Resend webhook event stored: type={}, emailId={}, from={}, to={}", eventType, emailId, fromEmail, toEmail);

            // 6. Handle side effects based on event type
            handleEventSideEffects(eventType, account, emailId, eventTimestamp);

            return true;

        } catch (Exception e) {
            log.error("Error processing Resend webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handle side effects for specific event types.
     *
     * <p>Two side-effects:
     * <ol>
     *   <li>Account-level counters (bounce count) — unchanged.</li>
     *   <li>Per-message delivery status on the matching {@link EmailMessage}
     *       row, looked up by {@code resend_email_id}. This is what surfaces
     *       in the email list UI ("Delivered", "Bounced" chips).</li>
     * </ol>
     *
     * <p>Webhooks can arrive out of order, so the stored status is decided by
     * {@link EmailDeliveryStatus#resolve} — the more actionable outcome wins,
     * whichever order the events turn up in — rather than by arrival sequence.
     * A late {@code delivery_delayed} cannot walk a delivered row backwards,
     * and a bounce is not buried by a delivery that preceded it.
     *
     * <p>That second case is not a hypothetical. A send to several recipients
     * produces one row and several outcomes, so one recipient's bounce used to
     * be discarded because another recipient's delivery had already landed.
     * The row then read "Delivered" for mail that someone never received, and
     * the per-recipient truth survived only in the webhook event table. The
     * timestamps are recorded regardless of which status wins, so a row can
     * show it was both delivered and bounced.
     */
    private void handleEventSideEffects(String eventType, EmailAccount account, String emailId, LocalDateTime eventTimestamp) {
        if (account != null && "email.bounced".equals(eventType)) {
            Long failedCount = account.getEmailsFailedCount() != null ? account.getEmailsFailedCount() : 0L;
            account.setEmailsFailedCount(failedCount + 1);
            emailAccountRepository.save(account);
        }

        if (emailId == null || emailId.isBlank()) return;

        // Primary lookup uses the dedicated column. For rows persisted before
        // the column existed, fall back to messageId="<emailId>" — the
        // wrapper format the sender service has always used — and back-fill
        // the new column so subsequent webhooks hit the fast path.
        EmailMessage match = emailMessageRepository.findByResendEmailId(emailId).orElse(null);
        if (match == null) {
            match = emailMessageRepository
                    .findByEmailAccountIdAndMessageId(
                            account != null ? account.getId() : -1L,
                            "<" + emailId + ">")
                    .orElse(null);
            if (match != null) {
                match.setResendEmailId(emailId);
            }
        }
        if (match == null) {
            log.debug("No EmailMessage row matched Resend emailId={}; event stored only", emailId);
            return;
        }

        LocalDateTime at = eventTimestamp != null ? eventTimestamp : LocalDateTime.now();
        EmailDeliveryStatus current = match.getDeliveryStatus();
        EmailDeliveryStatus incoming = null;
        boolean transitioned = true;

        // The timestamp of an outcome is recorded even when another outcome
        // holds the status, so "delivered to one, bounced for another" is not
        // lost just because one of the two has to win the single status field.
        switch (eventType) {
            case "email.delivered":
                incoming = EmailDeliveryStatus.DELIVERED;
                match.setDeliveredAt(at);
                break;
            case "email.bounced":
                incoming = EmailDeliveryStatus.BOUNCED;
                match.setBouncedAt(at);
                break;
            case "email.complained":
                incoming = EmailDeliveryStatus.COMPLAINED;
                match.setComplainedAt(at);
                break;
            case "email.delivery_delayed":
                incoming = EmailDeliveryStatus.DELIVERY_DELAYED;
                break;
            case "email.sent":
                incoming = EmailDeliveryStatus.SENT;
                break;
            default:
                log.debug("Unhandled Resend event type: {}", eventType);
                transitioned = false;
                break;
        }

        if (transitioned) {
            match.setDeliveryStatus(EmailDeliveryStatus.resolve(current, incoming));
            match.setLastEventType(eventType);
            emailMessageRepository.save(match);
            log.info("EmailMessage {} → {} (event {})",
                    match.getId(), match.getDeliveryStatus(), eventType);
        }
    }

    /**
     * Find a Resend email account by the sender email address.
     * Extracts the email from formats like "Name <email@example.com>".
     */
    private EmailAccount findResendAccountByEmail(String fromEmail) {
        if (fromEmail == null) return null;

        // Extract email from "Name <email@example.com>" format
        String email = fromEmail;
        if (fromEmail.contains("<") && fromEmail.contains(">")) {
            email = fromEmail.substring(fromEmail.indexOf("<") + 1, fromEmail.indexOf(">")).trim();
        }

        return emailAccountRepository.findByEmail(email)
            .filter(account -> account.getProviderType() == EmailAccountProvider.RESEND)
            .orElse(null);
    }
}
