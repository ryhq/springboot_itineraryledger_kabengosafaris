package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.Properties;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ImapCredentials;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ReceivingProtocol;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Put a copy of everything we send into the mailbox's own Sent folder.
 *
 * <p>Sending through an API rather than SMTP means the mail never passes through the mailbox, so
 * nothing puts a copy in Sent — that is normally a side effect of the mail client doing the
 * sending. The result on Kabengo: the panel listed 996 sent messages while the real mailbox held
 * 300, the newest dated 8 September, which were simply the ones migrated from the old host.
 * Around seven hundred messages to clients existed only in this application's database. Open
 * webmail, Outlook or a phone and they were not there, so anyone replying from a mail client had
 * no record of what had already been said — and a reply quoting nothing is how a client gets told
 * something twice, or contradicted.
 *
 * <p>Deliberately best-effort and asynchronous. The message has already been accepted by the
 * provider by the time this runs; failing the send because a copy could not be filed would turn
 * a cosmetic problem into a lost email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SentMailArchiveService {

    /**
     * Append a sent message to the account's Sent folder.
     *
     * @param account the sending account, which must also have IMAP configured
     * @param message the message as it was sent
     */
    @Async
    public void archiveAsync(EmailAccount account, MimeMessage message) {
        if (account == null || message == null) {
            return;
        }
        if (account.getReceivingProtocol() != ReceivingProtocol.IMAP
                || account.getImapHost() == null || account.getImapHost().isBlank()) {
            // POP3 cannot append, and an account with no mailbox has nowhere to file a copy.
            return;
        }

        Store store = null;
        try {
            store = connect(account);
            Folder sent = findSentFolder(store);
            if (sent == null) {
                log.warn("No Sent folder found for {}; the copy was not filed", account.getEmail());
                return;
            }
            if (!sent.isOpen()) {
                sent.open(Folder.READ_WRITE);
            }
            /*
             * Marked read on the way in. It is our own outgoing mail; arriving as an unread
             * message in Sent would put a bold count on a folder nobody needs to act on.
             */
            message.setFlag(Flags.Flag.SEEN, true);
            sent.appendMessages(new Message[] { message });
            sent.close(false);
            log.info("Filed a copy in {} for {}", sent.getFullName(), account.getEmail());
        } catch (Exception e) {
            log.warn("Could not file a sent copy for {}: {}", account.getEmail(), e.getMessage());
        } finally {
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (MessagingException ignored) {
                    // nothing useful to do, and the send already succeeded
                }
            }
        }
    }

    /**
     * The folder the server itself calls Sent.
     *
     * <p>Asks for the \Sent special-use attribute first rather than guessing a name: the folder
     * is "Sent" on Private Email but "Sent Items" on Exchange and "[Gmail]/Sent Mail" on Gmail,
     * and creating a second folder because the name did not match is worse than not filing at
     * all. Falls back to the common names, and gives up rather than inventing one.
     */
    private Folder findSentFolder(Store store) throws MessagingException {
        Folder root = store.getDefaultFolder();
        for (Folder f : root.list("*")) {
            try {
                if ((f.getType() & Folder.HOLDS_MESSAGES) == 0) {
                    continue;
                }
                for (String attr : attributesOf(f)) {
                    if ("\\Sent".equalsIgnoreCase(attr)) {
                        return f;
                    }
                }
            } catch (Exception ignored) {
                // a folder that cannot be inspected is simply not the one we want
            }
        }
        for (String name : new String[] { "Sent", "Sent Items", "Sent Messages", "INBOX.Sent" }) {
            Folder f = store.getFolder(name);
            if (f != null && f.exists()) {
                return f;
            }
        }
        return null;
    }

    /** Special-use attributes, when the implementation exposes them. */
    private String[] attributesOf(Folder f) {
        try {
            if (f instanceof org.eclipse.angus.mail.imap.IMAPFolder imap) {
                return imap.getAttributes();
            }
        } catch (Exception ignored) {
            // older servers do not advertise them; the name fallback covers that
        }
        return new String[0];
    }

    private Store connect(EmailAccount account) throws MessagingException {
        String protocol = Boolean.TRUE.equals(account.getImapUseSsl()) ? "imaps" : "imap";
        Properties props = new Properties();
        props.put("mail.store.protocol", protocol);
        props.put("mail." + protocol + ".host", account.getImapHost());
        props.put("mail." + protocol + ".port", String.valueOf(account.getImapPort()));
        if (Boolean.TRUE.equals(account.getImapUseTls())) {
            props.put("mail." + protocol + ".starttls.enable", "true");
        }
        props.put("mail." + protocol + ".connectiontimeout", "15000");
        props.put("mail." + protocol + ".timeout", "30000");

        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);
        ImapCredentials credentials = ImapCredentials.of(account);
        store.connect(account.getImapHost(), credentials.username(), credentials.password());
        return store;
    }
}
