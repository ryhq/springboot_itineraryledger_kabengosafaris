package com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity;

import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;

/**
 * Which login opens the mailbox.
 *
 * Most providers send and receive on one account, so one credential pair serves both and the IMAP
 * columns stay null. Some deployments cannot: the Jatelo droplet blocks outbound 465 and 587, so
 * that account sends through Brevo on 2525 and reads its mail from Namecheap Private Email — two
 * servers, two logins.
 *
 * This exists so the answer is computed in ONE place. It was computed in two: the fetch scheduler
 * and the "Test IMAP" button each built their own connection, which is how a test can pass while
 * every real fetch fails, or the reverse. Either way somebody trusts a green tick that means
 * nothing.
 *
 * The pair is all-or-nothing on purpose. A username without a password would otherwise be silently
 * combined with the SMTP password — a mixture nobody configured, failing with an authentication
 * error that names neither field. Create and update reject that combination outright.
 */
public record ImapCredentials(String username, String password) {

    /** The credentials to open {@code account}'s mailbox with, decrypted, ready to connect. */
    public static ImapCredentials of(EmailAccount account) {
        if (hasOwnCredentials(account)) {
            return new ImapCredentials(
                account.getImapUsername().trim(),
                EncryptionUtil.decrypt(account.getImapPassword()));
        }
        return new ImapCredentials(
            account.getSmtpUsername(),
            EncryptionUtil.decrypt(account.getSmtpPassword()));
    }

    /** True when the mailbox has a login of its own, rather than borrowing the sender's. */
    public static boolean hasOwnCredentials(EmailAccount account) {
        return notBlank(account.getImapUsername()) && notBlank(account.getImapPassword());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
