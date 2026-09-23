package com.itineraryledger.kabengosafaris.Initializers;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Properties;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailAttachmentRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailAttachment;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailStorageService;

import jakarta.mail.BodyPart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gives a paperclip something to point at.
 *
 * <p>A message carries TWO facts about its attachments: a boolean the list draws the paperclip
 * from, and the attachment rows the reader lists. Two writers set the boolean without writing the
 * rows — composing by hand never wrote them at all, and a sent PDF arriving without a filename
 * set the flag and skipped the row — so messages appeared to carry files and opened with nothing.
 * Twenty-three of them on Kabengo, one a proposal claiming four documents.
 *
 * <p>Both writers are fixed. This is for the messages already sitting in Sent, whose .eml files
 * are still on disk with the files inside them: it reads each one back, writes the rows, and where
 * the .eml genuinely has no attachment it clears the flag instead. Either way the two facts end up
 * telling the same story, which is the only outcome that matters.
 *
 * <p>Runs once in effect. It only looks at messages flagged as having attachments that have no
 * rows, so a repaired message is never visited again and every later boot is a no-op. Each message
 * is its own transaction inside its own try/catch, and the sweep is wrapped again: a backfill must
 * never be the reason the application will not start.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 40)
@RequiredArgsConstructor
@Slf4j
public class EmailAttachmentRowBackfillInitializer implements ApplicationRunner {

    private final EmailMessageRepository emailMessageRepository;
    /*
     * A separate bean, not a method on this one. @Transactional is applied by a proxy, and a
     * proxy is bypassed when an object calls its own method — so REQUIRES_NEW on a private helper
     * here would be silently ignored and the whole sweep would share one transaction, where a
     * single bad message takes the other twenty-two down with it.
     */
    private final AttachmentRowRepair repair;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<EmailMessage> lying = emailMessageRepository.findFlaggedWithNoAttachmentRows();
            if (lying.isEmpty()) return;

            int repaired = 0, cleared = 0, skipped = 0;
            for (EmailMessage message : lying) {
                try {
                    int written = repair.repairOne(message.getId());
                    if (written > 0) repaired++; else cleared++;
                } catch (Exception e) {
                    skipped++;
                    log.warn("Could not repair attachments on message {}: {}", message.getId(), e.getMessage());
                }
            }
            log.info("Attachment backfill: {} message(s) given their rows, {} had none and were "
                + "un-flagged, {} could not be read", repaired, cleared, skipped);
        } catch (Exception e) {
            log.error("Attachment backfill did not run: {}", e.getMessage());
        }
    }

    /** The per-message work, in its own bean so its transaction boundary is real. */
    @Component
    @RequiredArgsConstructor
    static class AttachmentRowRepair {

        private final EmailMessageRepository emailMessageRepository;
        private final EmailAttachmentRepository emailAttachmentRepository;
        private final EmailStorageService emailStorageService;

    /** @return how many rows were written; zero means the flag was cleared instead. */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public int repairOne(Long messageId) throws Exception {
            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null) return 0;

            byte[] raw = message.getFileName() == null ? null : emailStorageService.readEmlFile(
                message.getEmailAccount().getId(), message.getStoragePath(), message.getFileName());

            int written = 0;
            if (raw != null && raw.length > 0) {
                MimeMessage mime = new MimeMessage(
                    Session.getInstance(new Properties()), new ByteArrayInputStream(raw));
                written = walk(mime.getContent(), message);
            }

            /*
             * The flag follows the rows, never the other way round. A message whose .eml holds nothing
             * was mis-flagged when it was written, and saying so is better than leaving a paperclip
             * that opens onto an empty list.
             */
            message.setHasAttachments(written > 0);
            message.setAttachmentCount(written);
            emailMessageRepository.save(message);
            return written;
        }

        /**
         * Every attachment in the tree, however deeply nested.
         *
         * <p>Recursive because a message with both a body and files is multipart/mixed wrapping a
         * multipart/related wrapping a multipart/alternative: a top-level scan sees the wrapper and
         * walks past everything inside it.
         */
        private int walk(Object content, EmailMessage message) throws Exception {
            if (!(content instanceof MimeMultipart multipart)) return 0;
            int written = 0;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                Object inner = part.getContent();
                if (inner instanceof MimeMultipart) {
                    written += walk(inner, message);
                    continue;
                }
                boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
                    || part.getFileName() != null;
                if (!isAttachment) continue;

                String originalName = part.getFileName() != null ? part.getFileName() : "attachment_" + i;
                String storageName = message.getId() + "_" + originalName;
                byte[] bytes = part.getInputStream().readAllBytes();
                emailStorageService.saveAttachment(message.getEmailAccount().getId(), storageName, bytes);

                emailAttachmentRepository.save(EmailAttachment.builder()
                    .emailMessage(message)
                    .fileName(storageName)
                    .originalFileName(originalName)
                    .mimeType(part.getContentType())
                    .fileSize((long) bytes.length)
                    .storagePath("attachments")
                    .contentId(part.getHeader("Content-ID") != null ? part.getHeader("Content-ID")[0] : null)
                    .isInline("inline".equalsIgnoreCase(part.getDisposition()))
                    .build());
                written++;
            }
            return written;
        }
    }
}
