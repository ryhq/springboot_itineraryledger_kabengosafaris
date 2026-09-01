package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That a message without a Message-ID is fetched once, not once per cycle.
 *
 * Deduplication read `messageId != null && alreadyHave(messageId)`. A message whose header is
 * absent therefore skipped the check completely and was inserted again on every five-minute pass.
 * Jatelo's reservations mailbox reached ninety-three copies of one welcome letter inside a day of
 * being connected — every row stamped 05:13:50, every row with a null messageId — and was still
 * climbing when it was found.
 *
 * The header is optional in practice, so demanding one would fix nothing. The identity is derived
 * from the message instead.
 */
class MessagesWithoutAMessageIdTest {

    private static final Path SERVICE = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "EmailAccount/EmailMessage/Services/EmailReceivingService.java");

    @Test
    @DisplayName("the dedup check is no longer skipped when there is no Message-ID")
    void theCheckAlwaysRuns() throws IOException {
        String source = Files.readString(SERVICE);

        assertFalse(source.contains("messageId != null && emailMessageRepository"),
            "the null guard is back: a message without a Message-ID would skip deduplication"
                + " entirely and be re-inserted on every fetch");
        assertTrue(source.contains("String messageId = identityOf(mimeMessage)"),
            "the fetch loop must resolve an identity, not read the header directly");
    }

    @Test
    @DisplayName("the identity stored is the identity that was looked up")
    void bothSidesAgree() throws IOException {
        /*
         * The loop asks "do we have this one?" and processMessage writes the row. If they compute
         * the identity differently, every message is a miss on the next pass and the duplicates
         * come straight back — which is the same bug wearing a different hat.
         */
        String source = Files.readString(SERVICE);
        int callSites = source.split("identityOf\\(mimeMessage\\)", -1).length - 1;
        assertEquals(2, callSites,
            "exactly two places need the identity — the fetch loop that asks whether we have it"
                + " and processMessage that stores it. Found " + callSites + ".");
        assertFalse(source.contains("String messageId = mimeMessage.getMessageID();"),
            "processMessage is reading the raw header again, so it would store null where the"
                + " dedup looked for a derived id");
    }

    @Test
    @DisplayName("a derived identity says that it is derived")
    void syntheticIdsAreLabelled() throws IOException {
        String source = Files.readString(SERVICE);
        assertTrue(source.contains("@no-message-id.local"),
            "a synthetic id must be recognisable as one — nobody should mistake it for something"
                + " the sender wrote");
    }

    @Test
    @DisplayName("the identity is built from fields that do not change between fetches")
    void theMaterialIsStable() throws IOException {
        /*
         * Anything that varies per fetch — the receipt time, a folder position, the local file
         * name — would make every pass a fresh message and restore the duplicates exactly.
         */
        String source = Files.readString(SERVICE);
        int from = source.indexOf("private String identityOf");
        int to = source.indexOf("appendQuietly(StringBuilder", from);
        String helper = source.substring(from, to);

        for (String stable : new String[] {"getFrom()", "getRecipients", "getSubject", "getSentDate", "getSize"}) {
            assertTrue(helper.contains(stable), "the identity should include " + stable);
        }
        for (String unstable : new String[] {"LocalDateTime.now", "System.nanoTime", "getReceivedDate", "randomUUID"}) {
            assertFalse(helper.contains(unstable),
                unstable + " changes between fetches, so every pass would look like a new message");
        }
    }

    @Test
    @DisplayName("a bounce can say why, not only that it happened")
    void bounceReasonsAreReadable() throws IOException {
        /*
         * The provider sends type, subType and a message on every bounce. They were stored in
         * rawPayload and never read out, so the log could report 266 bounces and not one word
         * about why — and "Transient/General for six months" (a full mailbox) versus
         * "Permanent/NoEmail" (an address that does not exist) want opposite responses.
         */
        String dto = Files.readString(Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
            + "EmailAccount/ResendWebhook/ResendWebhookEventDTO.java"));
        for (String field : new String[] {"bounceType", "bounceSubType", "bounceMessage"}) {
            assertTrue(dto.contains(field), "the delivery event should expose " + field);
        }

        String service = Files.readString(Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
            + "EmailAccount/ResendWebhook/ResendWebhookEventGetService.java"));
        assertTrue(service.contains(".bounceType(bounce(event"),
            "convertToDTO must fill the bounce fields, or the DTO carries three permanent nulls");
    }
}
