package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendWebhookEventDTO {
    private String id;
    private String svixId;
    private String eventType;
    private String emailId;
    private String fromEmail;
    private String toEmail;
    private String subject;
    private LocalDateTime eventTimestamp;
    private LocalDateTime receivedAt;

    /**
     * Why it bounced, for the events that did.
     *
     * The provider says this on every bounce and it was stored in rawPayload and never read out,
     * so the log could report that 266 messages bounced and not one word about why. Answering
     * "is our mail broken?" then meant opening the provider's own dashboard.
     *
     * `Transient/General` repeated for six months to one address is a full or suspended mailbox;
     * `Permanent/NoEmail` is an address that does not exist. Those want opposite responses, and
     * the difference was invisible.
     */
    private String bounceType;

    private String bounceSubType;

    private String bounceMessage;
}
