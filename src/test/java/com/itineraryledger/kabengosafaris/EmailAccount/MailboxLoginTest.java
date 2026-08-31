package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ImapCredentials;

/**
 * Which login opens the mailbox, and that only one piece of code decides it.
 *
 * The Jatelo droplet blocks outbound 465 and 587 — both time out, and Brevo works only because it
 * listens on 2525 — so that account sends through a relay while its mail sits on Namecheap Private
 * Email. Two servers, two logins. Every other account still has one, so the fallback carries them.
 */
class MailboxLoginTest {

    private EmailAccount account(String imapUser, String imapSecret) {
        return EmailAccount.builder()
            .smtpUsername("b38de8001@smtp-brevo.com")
            .smtpPassword(EncryptionUtil.encrypt("the-relay-secret"))
            .imapUsername(imapUser)
            .imapPassword(imapSecret == null ? null : EncryptionUtil.encrypt(imapSecret))
            .build();
    }

    @Test
    @DisplayName("with no mailbox login of its own, the SMTP credentials are used")
    void fallsBackToTheSender() {
        ImapCredentials c = ImapCredentials.of(account(null, null));
        assertEquals("b38de8001@smtp-brevo.com", c.username());
        assertEquals("the-relay-secret", c.password());
        assertFalse(ImapCredentials.hasOwnCredentials(account(null, null)));
    }

    @Test
    @DisplayName("with its own login, the mailbox credentials win over the sender's")
    void prefersTheMailboxLogin() {
        ImapCredentials c = ImapCredentials.of(
            account("reservations@example.test", "the-mailbox-secret"));
        assertEquals("reservations@example.test", c.username());
        assertEquals("the-mailbox-secret", c.password(),
            "the relay's password would authenticate against the relay, not the mailbox");
    }

    @Test
    @DisplayName("half a login is ignored rather than mixed with the sender's other half")
    void halfALoginIsNotMixed() {
        /*
         * Sending the mailbox username with the RELAY's password is a combination nobody
         * configured; it fails with an authentication error naming neither field. Create and
         * update refuse the half-pair outright, and this is the belt to that braces: whatever
         * reaches the database, the resolver never invents a hybrid.
         */
        assertEquals("b38de8001@smtp-brevo.com",
            ImapCredentials.of(account("reservations@example.test", null)).username());
        assertEquals("b38de8001@smtp-brevo.com",
            ImapCredentials.of(account(null, "orphaned-secret")).username());
    }

    @Test
    @DisplayName("blank strings count as absent, not as an empty username")
    void blanksAreAbsent() {
        assertFalse(ImapCredentials.hasOwnCredentials(account("   ", "  ")));
        assertEquals("b38de8001@smtp-brevo.com", ImapCredentials.of(account("  ", "  ")).username());
    }

    @Test
    @DisplayName("both connection paths ask ImapCredentials — a test that passes must mean the fetch works")
    void onlyOnePlaceDecides() throws IOException {
        /*
         * The fetch scheduler and the "Test IMAP" button each built their own connection and each
         * hardcoded getSmtpUsername()/getSmtpPassword(). Two copies of one decision is how a green
         * tick comes to mean nothing: the button can pass while every real fetch fails, or the
         * reverse, and the panel reports the button.
         */
        for (Path p : new Path[] {
            Path.of("src/main/java/com/itineraryledger/kabengosafaris/EmailAccount/EmailMessage/"
                + "Services/EmailReceivingService.java"),
            Path.of("src/main/java/com/itineraryledger/kabengosafaris/EmailAccount/"
                + "EmailAccountServices/EmailAccountTestService.java"),
        }) {
            String source = Files.readString(p);
            Matcher connect = Pattern.compile("store\\.connect\\(([^;]*)\\)", Pattern.DOTALL)
                .matcher(source);
            int found = 0;
            while (connect.find()) {
                found++;
                String args = connect.group(1).replaceAll("\\s+", " ");
                assertTrue(args.contains("credentials.username()") && args.contains("credentials.password()"),
                    p.getFileName() + " opens the mailbox with " + args
                        + " — it must use ImapCredentials.of(account), or it will disagree with"
                        + " the other connection path");
            }
            assertTrue(found > 0, "no store.connect(...) found in " + p.getFileName()
                + " — this test is no longer watching what it thinks it is");
        }
    }

    @Test
    @DisplayName("the mailbox password is never returned, only whether one is set")
    void theSecretStaysWriteOnly() throws IOException {
        String dto = Files.readString(Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
            + "EmailAccount/DTOs/EmailAccountDTO.java"));
        assertTrue(dto.contains("imapPasswordConfigured"),
            "the response should say whether a mailbox password exists");
        assertFalse(dto.matches("(?s).*private String imapPassword;.*"),
            "the mailbox password must never be a field on the response DTO — the SMTP password,"
                + " API key and webhook secret are all write-only and this is no different");
    }
}
