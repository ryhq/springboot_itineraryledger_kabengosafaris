package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.SentMailArchiveService;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ReceivingProtocol;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Filing a copy in Sent must never be able to cost an email.
 *
 * <p>By the time this runs the provider has already accepted the message — it is away, and the
 * recipient will get it. Everything here is therefore best-effort: an unreachable mailbox, a
 * missing Sent folder, bad credentials or a server that refuses APPEND must all end as a log
 * line. Turning a cosmetic problem ("it is not in Sent") into a real one ("the send failed")
 * would be a far worse bug than the one this feature fixes.
 *
 * <p>Why the feature exists: sending through an API means the message never passes through the
 * mailbox, so nothing puts it in Sent. Kabengo's panel listed 996 sent messages while the real
 * mailbox held 300, newest dated 8 September — the ones migrated from the old host. Some seven
 * hundred messages to clients existed only in this application's database.
 */
@DisplayName("Filing a copy never costs an email")
class AFiledCopyNeverCostsAnEmailTest {

    private final SentMailArchiveService service = new SentMailArchiveService();

    private MimeMessage anyMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private EmailAccount account(ReceivingProtocol protocol, String host) {
        EmailAccount a = new EmailAccount();
        a.setEmail("info@example.com");
        a.setReceivingProtocol(protocol);
        a.setImapHost(host);
        a.setImapPort(993);
        a.setImapUseSsl(true);
        return a;
    }

    @Test
    @DisplayName("an account with no mailbox is skipped, not attempted")
    void noMailboxIsSkipped() {
        assertDoesNotThrow(() -> service.archiveAsync(account(ReceivingProtocol.IMAP, null), anyMessage()));
        assertDoesNotThrow(() -> service.archiveAsync(account(ReceivingProtocol.IMAP, "  "), anyMessage()));
        assertDoesNotThrow(() -> service.archiveAsync(account(ReceivingProtocol.NONE, "mail.example.com"), anyMessage()));
    }

    @Test
    @DisplayName("POP3 is skipped, because POP3 cannot append")
    void pop3IsSkipped() {
        assertDoesNotThrow(() -> service.archiveAsync(account(ReceivingProtocol.POP3, "mail.example.com"), anyMessage()));
    }

    @Test
    @DisplayName("null arguments are survivable")
    void nullsAreSurvivable() {
        assertDoesNotThrow(() -> service.archiveAsync(null, anyMessage()));
        assertDoesNotThrow(() -> service.archiveAsync(account(ReceivingProtocol.IMAP, "mail.example.com"), null));
        assertDoesNotThrow(() -> service.archiveAsync(null, null));
    }

    @Test
    @DisplayName("a mailbox that cannot be reached is a log line, not an exception")
    void unreachableMailboxDoesNotThrow() {
        // 127.0.0.1:993 has nothing listening in a test run; connection is refused quickly
        EmailAccount a = account(ReceivingProtocol.IMAP, "127.0.0.1");
        a.setImapUsername("info@example.com");
        a.setImapPassword("irrelevant");
        assertDoesNotThrow(() -> service.archiveAsync(a, anyMessage()));
    }
}
