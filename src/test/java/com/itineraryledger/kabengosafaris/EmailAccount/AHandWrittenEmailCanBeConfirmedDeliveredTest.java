package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A message typed by hand must be able to turn "Delivered", like every other one.
 *
 * <p>Found in the live Sent folder: the automated mail from this account (backup notices, the
 * quote-template send) showed <em>Delivered</em>, while two messages written by hand on the same
 * day to the same kind of address showed only <em>Sent</em>. The mail had arrived in both cases.
 *
 * <p>Delivery is only ever reported by the Resend webhook, which finds its row by
 * {@code resend_email_id}. {@code EmailSendingService} stores that id; {@code EmailComposeService}
 * called {@code resend.emails().send(...)}, wrote the returned id to the log and dropped it, so
 * nothing linked the confirmation back to the message. Its RFC-2822 message id matches neither the
 * webhook's fallback lookup nor the backfill's {@code <uuid>} pattern, so those rows could not be
 * repaired afterwards either: they were stranded on SENT for good.
 *
 * <p>This test reads the source, because the alternative is standing up Resend and a webhook
 * round-trip to assert one field is carried four lines.
 */
class AHandWrittenEmailCanBeConfirmedDeliveredTest {

    private static final Path COMPOSE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/EmailAccount/EmailMessage/Services/EmailComposeService.java");

    private String source() throws Exception {
        return Files.readString(COMPOSE);
    }

    @Test
    @DisplayName("the id Resend returns is stored on the row, not just logged")
    void theSentRowCarriesTheResendId() throws Exception {
        String src = source();

        assertTrue(src.contains(".resendEmailId(resendEmailId)"),
            "the sent copy must persist the Resend id, or the delivery webhook can never match it");

        assertTrue(src.contains("saveSentCopy(account, mimeMessage, dto, attachments, replyTo, response.getId())"),
            "the id returned by resend.emails().send(...) must reach saveSentCopy, not just the log");
    }

    @Test
    @DisplayName("the SMTP path stays on SENT, because nothing will ever confirm it")
    void theSmtpPathPassesNoId() throws Exception {
        assertTrue(source().contains("saveSentCopy(account, mimeMessage, dto, attachments, replyTo, null)"),
            "an SMTP send gets no delivery callback and must not claim one");
    }

    @Test
    @DisplayName("compose never awards DELIVERED to itself")
    void composeDoesNotClaimDelivery() throws Exception {
        String src = source();
        assertFalse(src.contains("EmailDeliveryStatus.DELIVERED"),
            "only the webhook may set DELIVERED; our server accepting a message proves nothing about receipt");
    }
}
