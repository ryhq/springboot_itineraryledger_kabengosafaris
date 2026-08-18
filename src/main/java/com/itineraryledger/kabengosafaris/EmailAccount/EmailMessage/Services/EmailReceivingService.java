package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services.AvailabilityRequestService;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailAttachmentRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.*;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ReceivingProtocol;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailReceivingService {

    private final EmailMessageRepository emailMessageRepository;
    private final EmailFolderRepository emailFolderRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailStorageService emailStorageService;
    private final EmailSettingGetterServices emailSettingGetterServices;
    private final EmailContactService emailContactService;
    /** availability requests notice their own replies — see the call after a message is saved */
    private final AvailabilityRequestService availabilityRequestService;
    private final ObjectMapper objectMapper;

    /**
     * Fetch new emails for an account (IMAP/POP3).
     *
     * Strategy:
     * - First sync (lastFetchedAt == null): fetch emails from the last N days
     *   (configured via email.fetch.initial.sync.days, default 30)
     * - Subsequent syncs: fetch emails received since lastFetchedAt
     * - Deduplicates by Message-ID to avoid re-processing
     * - Respects server SEEN flag for read/unread state
     * - Caps at maxFetchCount per cycle to avoid memory issues
     */
    @Transactional
    public int fetchNewEmails(EmailAccount account) {
        if (account.getReceivingProtocol() == ReceivingProtocol.NONE) {
            return 0;
        }

        int fetchedCount = 0;
        Store store = null;

        try {
            store = connectToStore(account);
            Folder remoteInbox = store.getFolder("INBOX");
            remoteInbox.open(Folder.READ_ONLY);

            EmailFolder localInbox = emailFolderRepository
                .findByEmailAccountIdAndType(account.getId(), EmailFolderType.INBOX)
                .orElse(null);

            if (localInbox == null) {
                log.warn("No local INBOX folder found for account {}", account.getEmail());
                return 0;
            }

            int maxFetch = emailSettingGetterServices.getMaxFetchCount();

            // Determine which messages to fetch
            Message[] messages = getMessagesToFetch(remoteInbox, account, maxFetch);

            log.info("Found {} candidate messages for account {} (max: {})",
                messages.length, account.getEmail(), maxFetch);

            for (Message message : messages) {
                MimeMessage mimeMessage = (MimeMessage) message;
                String messageId = mimeMessage.getMessageID();

                // Skip if already fetched (deduplicate by Message-ID)
                if (messageId != null && emailMessageRepository
                        .findByEmailAccountIdAndMessageId(account.getId(), messageId).isPresent()) {
                    continue;
                }

                try {
                    // Read the SEEN flag from server before processing
                    boolean isSeenOnServer = message.isSet(Flags.Flag.SEEN);
                    processMessage(mimeMessage, account, localInbox, isSeenOnServer);
                    fetchedCount++;
                } catch (Exception e) {
                    log.warn("Failed to process message {}: {}", messageId, e.getMessage());
                }
            }

            remoteInbox.close(false);

            // Update account stats
            account.setLastFetchedAt(LocalDateTime.now());
            account.setLastFetchErrorMessage(null);
            account.setEmailsReceivedCount(account.getEmailsReceivedCount() + fetchedCount);
            emailAccountRepository.save(account);

            log.info("Fetched {} new emails for account {}", fetchedCount, account.getEmail());

        } catch (Exception e) {
            log.error("Failed to fetch emails for account {}: {}", account.getEmail(), e.getMessage());
            account.setLastFetchErrorMessage(e.getMessage());
            emailAccountRepository.save(account);
        } finally {
            if (store != null && store.isConnected()) {
                try { store.close(); } catch (Exception ignored) {}
            }
        }

        return fetchedCount;
    }

    /**
     * Determine which messages to fetch based on sync state.
     *
     * - First sync: IMAP SINCE search for last N days (email.fetch.initial.sync.days)
     * - Subsequent syncs: IMAP SINCE search from lastFetchedAt
     * - POP3: falls back to tail-N approach (POP3 doesn't support search)
     * - Always capped at maxFetch
     */
    private Message[] getMessagesToFetch(Folder remoteInbox, EmailAccount account, int maxFetch)
            throws MessagingException {

        // POP3 doesn't support IMAP search — fall back to tail-N
        if (account.getReceivingProtocol() == ReceivingProtocol.POP3) {
            return getMessagesByTail(remoteInbox, maxFetch);
        }

        // IMAP: use SINCE search
        Date sinceDate;
        if (account.getLastFetchedAt() == null) {
            // First sync — go back N days
            int initialSyncDays = emailSettingGetterServices.getInitialSyncDays();
            sinceDate = Date.from(LocalDateTime.now().minusDays(initialSyncDays)
                .atZone(ZoneId.systemDefault()).toInstant());
            log.info("First sync for account {} — fetching emails from last {} days",
                account.getEmail(), initialSyncDays);
        } else {
            // Subsequent sync — fetch since last successful fetch
            sinceDate = Date.from(account.getLastFetchedAt()
                .atZone(ZoneId.systemDefault()).toInstant());
            log.info("Incremental sync for account {} — fetching emails since {}",
                account.getEmail(), account.getLastFetchedAt());
        }

        SearchTerm sinceTerm = new ReceivedDateTerm(ComparisonTerm.GE, sinceDate);
        Message[] messages = remoteInbox.search(sinceTerm);

        // Cap at maxFetch (take the most recent ones)
        if (messages.length > maxFetch) {
            Message[] capped = new Message[maxFetch];
            System.arraycopy(messages, messages.length - maxFetch, capped, 0, maxFetch);
            return capped;
        }

        return messages;
    }

    /**
     * Fallback for POP3: get last N messages from folder.
     */
    private Message[] getMessagesByTail(Folder folder, int maxFetch) throws MessagingException {
        Message[] allMessages = folder.getMessages();
        if (allMessages.length <= maxFetch) {
            return allMessages;
        }
        int start = allMessages.length - maxFetch;
        Message[] tail = new Message[maxFetch];
        System.arraycopy(allMessages, start, tail, 0, maxFetch);
        return tail;
    }

    private Store connectToStore(EmailAccount account) throws MessagingException {
        Properties props = new Properties();
        String protocol;

        if (account.getReceivingProtocol() == ReceivingProtocol.IMAP) {
            protocol = Boolean.TRUE.equals(account.getImapUseSsl()) ? "imaps" : "imap";
            props.put("mail.store.protocol", protocol);
            props.put("mail." + protocol + ".host", account.getImapHost());
            props.put("mail." + protocol + ".port", String.valueOf(account.getImapPort()));
            if (Boolean.TRUE.equals(account.getImapUseTls())) {
                props.put("mail." + protocol + ".starttls.enable", "true");
            }
        } else {
            protocol = Boolean.TRUE.equals(account.getImapUseSsl()) ? "pop3s" : "pop3";
            props.put("mail.store.protocol", protocol);
            props.put("mail." + protocol + ".host", account.getImapHost());
            props.put("mail." + protocol + ".port", String.valueOf(account.getImapPort()));
        }

        props.put("mail." + protocol + ".connectiontimeout", "15000");
        props.put("mail." + protocol + ".timeout", "30000");

        Session session = Session.getInstance(props);
        Store store = session.getStore(protocol);
        String decryptedPassword = EncryptionUtil.decrypt(account.getSmtpPassword());
        store.connect(account.getImapHost(), account.getSmtpUsername(), decryptedPassword);

        return store;
    }

    private void processMessage(MimeMessage mimeMessage, EmailAccount account, EmailFolder folder,
                                boolean isSeenOnServer) throws Exception {
        String messageId = mimeMessage.getMessageID();
        String fileName = emailStorageService.generateEmlFileName(messageId);

        // Save .eml file to disk
        emailStorageService.saveEmlFromMimeMessage(account.getId(), folder.getName().toLowerCase(), fileName, mimeMessage);

        // Extract metadata
        String fromAddress = null;
        String fromName = null;
        Address[] fromAddresses = mimeMessage.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            InternetAddress ia = (InternetAddress) fromAddresses[0];
            fromAddress = ia.getAddress();
            fromName = ia.getPersonal();
        }

        String[] toArray = extractAddresses(mimeMessage.getRecipients(Message.RecipientType.TO));
        String[] ccArray = extractAddresses(mimeMessage.getRecipients(Message.RecipientType.CC));

        String inReplyTo = mimeMessage.getHeader("In-Reply-To") != null
            ? mimeMessage.getHeader("In-Reply-To")[0] : null;
        String references = mimeMessage.getHeader("References") != null
            ? mimeMessage.getHeader("References")[0] : null;

        // Thread ID: use In-Reply-To or messageId itself
        String threadId = inReplyTo != null ? inReplyTo : messageId;

        // Extract snippet
        String snippet = extractPlainTextSnippet(mimeMessage, 200);

        // Check for attachments
        boolean hasAttachments = false;
        int attachmentCount = 0;
        if (mimeMessage.getContentType() != null && mimeMessage.getContentType().toLowerCase().contains("multipart")) {
            Object content = mimeMessage.getContent();
            if (content instanceof MimeMultipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part.getFileName() != null) {
                        hasAttachments = true;
                        attachmentCount++;
                    }
                }
            }
        }

        // Calculate file size
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mimeMessage.writeTo(baos);
        long fileSize = baos.size();

        LocalDateTime sentAt = mimeMessage.getSentDate() != null
            ? mimeMessage.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            : LocalDateTime.now();

        // Create DB record — isRead respects server SEEN flag
        EmailMessage emailMessage = EmailMessage.builder()
            .emailAccount(account)
            .folder(folder)
            .messageId(messageId)
            .inReplyTo(inReplyTo)
            .references(references)
            .threadId(threadId)
            .fromAddress(fromAddress)
            .fromName(fromName)
            .toAddresses(toArray != null ? objectMapper.writeValueAsString(toArray) : null)
            .ccAddresses(ccArray != null ? objectMapper.writeValueAsString(ccArray) : null)
            .subject(mimeMessage.getSubject())
            .snippet(snippet)
            .isRead(isSeenOnServer)
            .isStarred(false)
            .isDraft(false)
            .hasAttachments(hasAttachments)
            .attachmentCount(attachmentCount)
            .fileName(fileName)
            .fileSize(fileSize)
            .storagePath(folder.getName().toLowerCase())
            .sentAt(sentAt)
            .receivedAt(LocalDateTime.now())
            .imapUid(String.valueOf(mimeMessage.getMessageNumber()))
            .build();

        emailMessage = emailMessageRepository.save(emailMessage);

        /*
         * Does this answer something we asked?
         *
         * Headers first — In-Reply-To, References, thread — then the subject and the sender, because
         * a lodge replying from reservations@ through a client that drops In-Reply-To is ordinary,
         * and a request left on the chase list after it has been answered is the thing this exists
         * to prevent. Best effort by construction: a mailbox sync must never fail because of
         * bookkeeping, which is why the service swallows its own errors rather than letting one
         * message abort the fetch.
         */
        availabilityRequestService.noticeIncomingMessage(
            emailMessage.getId(), inReplyTo, references, threadId,
            emailMessage.getFromAddress(), emailMessage.getSubject(), emailMessage.getReceivedAt());

        // Extract and save attachments
        if (hasAttachments) {
            extractAndSaveAttachments(mimeMessage, emailMessage, account.getId());
        }

        // Update folder counts
        emailFolderRepository.incrementMessageCount(folder.getId(), 1);
        if (!isSeenOnServer) {
            emailFolderRepository.incrementUnreadCount(folder.getId(), 1);
        }

        // Auto-harvest sender as contact
        emailContactService.harvestContact(account, fromAddress, fromName,
            EmailContact.ContactSource.RECEIVED);

        // Auto-harvest TO and CC recipients from received emails
        if (toArray != null) {
            emailContactService.harvestContacts(account, Arrays.asList(toArray), EmailContact.ContactSource.RECEIVED);
        }
        if (ccArray != null) {
            emailContactService.harvestContacts(account, Arrays.asList(ccArray), EmailContact.ContactSource.CC);
        }
    }

    private String[] extractAddresses(Address[] addresses) {
        if (addresses == null) return null;
        String[] result = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            result[i] = ((InternetAddress) addresses[i]).getAddress();
        }
        return result;
    }

    private String extractPlainTextSnippet(MimeMessage message, int maxLength) {
        try {
            Object content = message.getContent();
            if (content instanceof String text) {
                return text.length() > maxLength ? text.substring(0, maxLength) : text;
            }
            if (content instanceof MimeMultipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        String text = (String) part.getContent();
                        return text.length() > maxLength ? text.substring(0, maxLength) : text;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract text snippet: {}", e.getMessage());
        }
        return null;
    }

    private void extractAndSaveAttachments(MimeMessage message, EmailMessage emailMessage, Long accountId) {
        try {
            Object content = message.getContent();
            if (content instanceof MimeMultipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part.getFileName() != null) {
                        String originalName = part.getFileName();
                        if (originalName == null) originalName = "attachment_" + i;

                        String storageName = emailMessage.getId() + "_" + originalName;

                        try (InputStream is = part.getInputStream()) {
                            byte[] bytes = is.readAllBytes();
                            emailStorageService.saveAttachment(accountId, storageName, bytes);

                            EmailAttachment attachment = EmailAttachment.builder()
                                .emailMessage(emailMessage)
                                .fileName(storageName)
                                .originalFileName(originalName)
                                .mimeType(part.getContentType())
                                .fileSize((long) bytes.length)
                                .storagePath("attachments")
                                .contentId(part.getHeader("Content-ID") != null ? part.getHeader("Content-ID")[0] : null)
                                .isInline("inline".equalsIgnoreCase(part.getDisposition()))
                                .build();
                            emailAttachmentRepository.save(attachment);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract attachments for message {}: {}", emailMessage.getMessageId(), e.getMessage());
        }
    }
}
