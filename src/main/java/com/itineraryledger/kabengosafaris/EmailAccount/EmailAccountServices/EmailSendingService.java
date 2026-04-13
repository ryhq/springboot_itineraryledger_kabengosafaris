package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.Session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.SendingMethod;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailAttachmentRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailAttachment;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolder;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolderType;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailSettingGetterServices;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailStorageService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * EmailSendingService - Service for sending emails using configured email accounts
 *
 * This service handles:
 * - Sending HTML emails using the default enabled email account
 * - Creating and configuring JavaMailSender instances
 * - Tracking email send statistics (sent count and failed count)
 * - Handling email sending failures gracefully
 */
@Service
@Slf4j
public class EmailSendingService {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailStorageService emailStorageService;
    private final EmailSettingGetterServices emailSettingGetterServices;
    private final EmailFolderRepository emailFolderRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public EmailSendingService(
            EmailAccountRepository emailAccountRepository,
            EmailStorageService emailStorageService,
            EmailSettingGetterServices emailSettingGetterServices,
            EmailFolderRepository emailFolderRepository,
            EmailMessageRepository emailMessageRepository,
            EmailAttachmentRepository emailAttachmentRepository,
            ObjectMapper objectMapper) {
        this.emailAccountRepository = emailAccountRepository;
        this.emailStorageService = emailStorageService;
        this.emailSettingGetterServices = emailSettingGetterServices;
        this.emailFolderRepository = emailFolderRepository;
        this.emailMessageRepository = emailMessageRepository;
        this.emailAttachmentRepository = emailAttachmentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Send an HTML email using the default enabled email account (asynchronously)
     * Automatically updates the account's sent/failed counters
     *
     * This method runs asynchronously to avoid blocking the calling thread.
     * Perfect for sending registration emails without delaying the registration response.
     *
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param htmlContent HTML content of the email
     * @throws RuntimeException if no enabled account exists or sending fails
     */
    @Async
    @Transactional
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        log.debug("Attempting to send HTML email to: {}", toEmail);

        EmailAccount emailAccount = null;

        try {
            // Get the first enabled and default email account
            emailAccount = emailAccountRepository
                .findFirstByEnabledTrueAndIsDefaultTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new RuntimeException(
                    "No enabled and default email account found. Please configure an email account first."
                ));

            log.info("Using email account: {} ({}) [{}] to send email",
                emailAccount.getName(), emailAccount.getEmail(), emailAccount.getProviderType());

            if (emailAccount.getProviderType() == EmailAccountProvider.RESEND
                    && emailAccount.getSendingMethod() == SendingMethod.API) {
                sendViaResend(emailAccount, toEmail, subject, htmlContent);
            } else if (emailAccount.getProviderType() == EmailAccountProvider.RESEND
                    && emailAccount.getSendingMethod() == SendingMethod.SMTP) {
                sendViaResendSmtp(emailAccount, toEmail, subject, htmlContent);
            } else {
                sendViaSmtp(emailAccount, toEmail, subject, htmlContent);
            }

            // Update sent counter on success
            incrementSentCount(emailAccount);

            log.info("HTML email sent successfully to: {} using account: {}", toEmail, emailAccount.getName());

        } catch (Exception e) {
            // Update failed counter on failure
            if (emailAccount != null) {
                incrementFailedCount(emailAccount);
            }

            log.error("Failed to send HTML email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send an HTML email with a PDF attachment using the default enabled email account (asynchronously)
     *
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param htmlContent HTML content of the email
     * @param pdfBytes PDF file bytes to attach
     * @param pdfFileName Filename for the PDF attachment
     */
    @Async
    @Transactional
    public void sendHtmlEmailWithAttachment(String toEmail, String subject, String htmlContent, byte[] pdfBytes, String pdfFileName) {
        log.debug("Attempting to send HTML email with attachment to: {}", toEmail);

        EmailAccount emailAccount = null;

        try {
            emailAccount = emailAccountRepository
                .findFirstByEnabledTrueAndIsDefaultTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new RuntimeException(
                    "No enabled and default email account found. Please configure an email account first."
                ));

            log.info("Using email account: {} ({}) [{}] to send email with attachment",
                emailAccount.getName(), emailAccount.getEmail(), emailAccount.getProviderType());

            if (emailAccount.getProviderType() == EmailAccountProvider.RESEND
                    && emailAccount.getSendingMethod() == SendingMethod.API) {
                sendViaResendWithAttachment(emailAccount, toEmail, subject, htmlContent, pdfBytes, pdfFileName);
            } else {
                sendViaSmtpWithAttachment(emailAccount, toEmail, subject, htmlContent, pdfBytes, pdfFileName);
            }

            incrementSentCount(emailAccount);
            log.info("HTML email with attachment sent successfully to: {} using account: {}", toEmail, emailAccount.getName());

        } catch (Exception e) {
            if (emailAccount != null) {
                incrementFailedCount(emailAccount);
            }
            log.error("Failed to send HTML email with attachment to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Send email via Resend API with PDF attachment
     */
    private void sendViaResendWithAttachment(EmailAccount emailAccount, String toEmail, String subject, String htmlContent, byte[] pdfBytes, String pdfFileName) throws ResendException {
        String apiKey = EncryptionUtil.decrypt(emailAccount.getApiKey());
        Resend resend = new Resend(apiKey);

        Attachment attachment = Attachment.builder()
            .fileName(pdfFileName)
            .content(Base64.getEncoder().encodeToString(pdfBytes))
            .build();

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from(emailAccount.getName() + " <" + emailAccount.getEmail() + ">")
            .to(toEmail)
            .subject(subject)
            .html(htmlContent)
            .addAttachment(attachment)
            .build();

        CreateEmailResponse response = resend.emails().send(params);
        log.info("Email with attachment sent via Resend API. Email ID: {}", response.getId());

        captureResendSentEmail(emailAccount, response.getId(), toEmail, subject, htmlContent, pdfBytes, pdfFileName);
    }

    /**
     * Send email via SMTP with PDF attachment
     */
    private void sendViaSmtpWithAttachment(EmailAccount emailAccount, String toEmail, String subject, String htmlContent, byte[] pdfBytes, String pdfFileName) throws Exception {
        JavaMailSender mailSender = createMailSender(emailAccount);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(emailAccount.getEmail(), emailAccount.getName());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.addAttachment(pdfFileName, () -> new java.io.ByteArrayInputStream(pdfBytes), "application/pdf");

        mailSender.send(mimeMessage);
        log.info("Email with attachment sent via SMTP to: {}", toEmail);

        captureSentEmail(emailAccount, mimeMessage, toEmail, subject, htmlContent, pdfBytes, pdfFileName);
    }

    /**
     * Send email via Resend HTTP API
     */
    private void sendViaResend(EmailAccount emailAccount, String toEmail, String subject, String htmlContent) throws ResendException {
        String apiKey = EncryptionUtil.decrypt(emailAccount.getApiKey());
        Resend resend = new Resend(apiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from(emailAccount.getName() + " <" + emailAccount.getEmail() + ">")
            .to(toEmail)
            .subject(subject)
            .html(htmlContent)
            .build();

        CreateEmailResponse response = resend.emails().send(params);
        String resendEmailId = response.getId();
        log.info("Email sent via Resend API. Email ID: {}", resendEmailId);

        // Capture sent email inline (runs within the @Async @Transactional context)
        captureResendSentEmail(emailAccount, resendEmailId, toEmail, subject, htmlContent, null, null);
    }

    /**
     * Send email via Resend SMTP gateway (smtp.resend.com)
     * Uses the API key as SMTP password, with fixed host/username
     */
    private void sendViaResendSmtp(EmailAccount emailAccount, String toEmail, String subject, String htmlContent) throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // Resend SMTP config: host and username from entity (auto-filled), password = API key
        sender.setHost(emailAccount.getSmtpHost() != null ? emailAccount.getSmtpHost() : "smtp.resend.com");
        sender.setPort(emailAccount.getSmtpPort() != null ? emailAccount.getSmtpPort() : 465);
        sender.setUsername("resend");

        // Use decrypted API key as SMTP password
        String apiKey = EncryptionUtil.decrypt(emailAccount.getApiKey());
        sender.setPassword(apiKey);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        Boolean useTls = emailAccount.getUseTls() != null ? emailAccount.getUseTls() : false;
        Boolean useSsl = emailAccount.getUseSsl() != null ? emailAccount.getUseSsl() : true;
        props.put("mail.smtp.starttls.enabled", useTls);
        props.put("mail.smtp.starttls.required", useTls);
        props.put("mail.smtp.ssl.enable", useSsl);

        if (useSsl) {
            props.put("mail.smtp.socketFactory.protocol", "SSLv23");
            props.put("mail.smtp.socketFactory.port", sender.getPort());
        }

        props.put("mail.smtp.connectiontimeout", 10000);
        props.put("mail.smtp.timeout", 10000);
        props.put("mail.smtp.writetimeout", 10000);

        log.debug("Resend SMTP configured: host={}, port={}, useTls={}, useSsl={}",
                sender.getHost(), sender.getPort(), useTls, useSsl);

        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(emailAccount.getEmail(), emailAccount.getName());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        sender.send(mimeMessage);
        log.info("Email sent via Resend SMTP gateway to: {}", toEmail);

        // Capture sent email inline (runs within the @Async @Transactional context)
        captureSentEmail(emailAccount, mimeMessage, toEmail, subject, htmlContent, null, null);
    }

    /**
     * Send email via SMTP (existing logic)
     */
    private void sendViaSmtp(EmailAccount emailAccount, String toEmail, String subject, String htmlContent) throws Exception {
        // Create mail sender with account configuration
        JavaMailSender mailSender = createMailSender(emailAccount);

        // Create and send the email
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        // Set from address with display name
        helper.setFrom(emailAccount.getEmail(), emailAccount.getName());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        mailSender.send(mimeMessage);

        // Capture sent email inline (runs within the @Async @Transactional context)
        captureSentEmail(emailAccount, mimeMessage, toEmail, subject, htmlContent, null, null);
    }

    /**
     * Increment the sent emails counter for an email account
     *
     * @param emailAccount The email account to update
     */
    private void incrementSentCount(EmailAccount emailAccount) {
        Long currentCount = emailAccount.getEmailsSentCount() != null ? emailAccount.getEmailsSentCount() : 0L;
        emailAccount.setEmailsSentCount(currentCount + 1);
        emailAccountRepository.save(emailAccount);
        log.debug("Incremented sent count for account {}: {} -> {}",
            emailAccount.getName(), currentCount, currentCount + 1);
    }

    /**
     * Increment the failed emails counter for an email account
     *
     * @param emailAccount The email account to update
     */
    private void incrementFailedCount(EmailAccount emailAccount) {
        Long currentCount = emailAccount.getEmailsFailedCount() != null ? emailAccount.getEmailsFailedCount() : 0L;
        emailAccount.setEmailsFailedCount(currentCount + 1);
        emailAccountRepository.save(emailAccount);
        log.debug("Incremented failed count for account {}: {} -> {}",
            emailAccount.getName(), currentCount, currentCount + 1);
    }

    /**
     * Capture a sent email as .eml file and store metadata in DB.
     *
     * @param account       The sending email account
     * @param mimeMessage   The sent MimeMessage (contains full HTML + attachments)
     * @param toEmail       Recipient email
     * @param subject       Email subject
     * @param htmlContent   The rendered HTML body (for snippet extraction)
     * @param pdfBytes      PDF attachment bytes (nullable if no attachment)
     * @param pdfFileName   PDF attachment filename (nullable)
     */
    private void captureSentEmail(EmailAccount account, MimeMessage mimeMessage, String toEmail, String subject,
                                   String htmlContent, byte[] pdfBytes, String pdfFileName) {
        try {
            if (!Boolean.TRUE.equals(emailSettingGetterServices.isSentCaptureEnabled())) {
                return;
            }

            Long accountId = account.getId();
            String messageId = mimeMessage.getMessageID();
            String fileName = emailStorageService.generateEmlFileName(messageId);

            // Save .eml to disk (contains full HTML body + attachments)
            emailStorageService.saveEmlFromMimeMessage(accountId, "sent", fileName, mimeMessage);

            // Get sent folder
            EmailFolder sentFolder = emailFolderRepository
                .findByEmailAccountIdAndType(accountId, EmailFolderType.SENT)
                .orElse(null);

            if (sentFolder == null) {
                log.debug("No SENT folder found for account {} — skipping capture", account.getEmail());
                return;
            }

            // Calculate file size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeMessage.writeTo(baos);

            // Extract snippet from HTML body, falling back to subject
            String snippet = extractSnippet(htmlContent, 200);
            if (snippet == null || snippet.isBlank()) {
                snippet = subject != null && subject.length() > 200 ? subject.substring(0, 200) : subject;
            }

            boolean hasAttachment = pdfBytes != null;

            // Create metadata record
            EmailMessage emailMessage = EmailMessage.builder()
                .emailAccount(account)
                .folder(sentFolder)
                .messageId(messageId)
                .fromAddress(account.getEmail())
                .fromName(account.getName())
                .toAddresses(objectMapper.writeValueAsString(List.of(toEmail)))
                .subject(subject)
                .snippet(snippet)
                .isRead(true)
                .isDraft(false)
                .hasAttachments(hasAttachment)
                .attachmentCount(hasAttachment ? 1 : 0)
                .fileName(fileName)
                .fileSize((long) baos.size())
                .storagePath("sent")
                .sentAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();

            emailMessageRepository.save(emailMessage);

            // Save PDF attachment to disk and create attachment record
            if (pdfBytes != null && pdfFileName != null) {
                String attachStorageName = System.currentTimeMillis() + "_" + pdfFileName;
                emailStorageService.saveAttachment(accountId, attachStorageName, pdfBytes);

                EmailAttachment attachment = EmailAttachment.builder()
                    .emailMessage(emailMessage)
                    .fileName(attachStorageName)
                    .originalFileName(pdfFileName)
                    .mimeType("application/pdf")
                    .fileSize((long) pdfBytes.length)
                    .storagePath("attachments")
                    .isInline(false)
                    .build();
                emailAttachmentRepository.save(attachment);
            }

            emailFolderRepository.incrementMessageCount(sentFolder.getId(), 1);

            log.debug("Captured sent email to {} in SENT folder for account {}", toEmail, account.getEmail());
        } catch (Exception e) {
            log.warn("Failed to capture sent email for account {}: {}", account.getEmail(), e.getMessage());
        }
    }

    /**
     * Capture a Resend API-sent email by constructing a .eml file from the HTML content
     * and saving it to disk, so it can be viewed like any other sent email.
     *
     * @param account       The sending email account
     * @param resendEmailId Resend API email ID
     * @param toEmail       Recipient email
     * @param subject       Email subject
     * @param htmlContent   The rendered HTML body
     * @param pdfBytes      PDF attachment bytes (nullable)
     * @param pdfFileName   PDF attachment filename (nullable)
     */
    private void captureResendSentEmail(EmailAccount account, String resendEmailId, String toEmail, String subject,
                                         String htmlContent, byte[] pdfBytes, String pdfFileName) {
        try {
            if (!Boolean.TRUE.equals(emailSettingGetterServices.isSentCaptureEnabled())) {
                return;
            }

            Long accountId = account.getId();

            // Get sent folder
            EmailFolder sentFolder = emailFolderRepository
                .findByEmailAccountIdAndType(accountId, EmailFolderType.SENT)
                .orElse(null);

            if (sentFolder == null) {
                log.debug("No SENT folder found for account {} — skipping Resend capture", account.getEmail());
                return;
            }

            // Build a MimeMessage from the HTML content so we can save a proper .eml file
            Session session = Session.getInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,
                pdfBytes != null, "UTF-8");

            helper.setFrom(account.getEmail(), account.getName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (pdfBytes != null && pdfFileName != null) {
                helper.addAttachment(pdfFileName,
                    () -> new ByteArrayInputStream(pdfBytes), "application/pdf");
            }

            // Use Resend email ID as message ID
            mimeMessage.setHeader("Message-ID", "<" + resendEmailId + ">");
            mimeMessage.saveChanges();

            String fileName = emailStorageService.generateEmlFileName(resendEmailId);

            // Save .eml to disk
            emailStorageService.saveEmlFromMimeMessage(accountId, "sent", fileName, mimeMessage);

            // Calculate file size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeMessage.writeTo(baos);

            // Extract snippet from HTML body
            boolean hasAttachment = pdfBytes != null;
            String snippet = extractSnippet(htmlContent, 200);
            if (snippet == null || snippet.isBlank()) {
                snippet = subject != null && subject.length() > 200 ? subject.substring(0, 200) : subject;
            }

            // Create metadata record
            EmailMessage emailMessage = EmailMessage.builder()
                .emailAccount(account)
                .folder(sentFolder)
                .messageId("<" + resendEmailId + ">")
                .fromAddress(account.getEmail())
                .fromName(account.getName())
                .toAddresses(objectMapper.writeValueAsString(List.of(toEmail)))
                .subject(subject)
                .snippet(snippet)
                .isRead(true)
                .isDraft(false)
                .hasAttachments(hasAttachment)
                .attachmentCount(hasAttachment ? 1 : 0)
                .fileName(fileName)
                .fileSize((long) baos.size())
                .storagePath("sent")
                .sentAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();

            emailMessageRepository.save(emailMessage);

            // Save PDF attachment to disk and create attachment record
            if (pdfBytes != null && pdfFileName != null) {
                String attachStorageName = System.currentTimeMillis() + "_" + pdfFileName;
                emailStorageService.saveAttachment(accountId, attachStorageName, pdfBytes);

                EmailAttachment attachment = EmailAttachment.builder()
                    .emailMessage(emailMessage)
                    .fileName(attachStorageName)
                    .originalFileName(pdfFileName)
                    .mimeType("application/pdf")
                    .fileSize((long) pdfBytes.length)
                    .storagePath("attachments")
                    .isInline(false)
                    .build();
                emailAttachmentRepository.save(attachment);
            }

            emailFolderRepository.incrementMessageCount(sentFolder.getId(), 1);

            log.debug("Captured Resend sent email to {} in SENT folder for account {}", toEmail, account.getEmail());
        } catch (Exception e) {
            log.warn("Failed to capture Resend sent email for account {}: {}", account.getEmail(), e.getMessage());
        }
    }

    /**
     * Create a JavaMailSender configured with the email account settings
     *
     * @param emailAccount The email account configuration
     * @return Configured JavaMailSender instance
     */
    private JavaMailSender createMailSender(EmailAccount emailAccount) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // Set SMTP configuration
        sender.setHost(emailAccount.getSmtpHost());
        sender.setPort(emailAccount.getSmtpPort());
        sender.setUsername(emailAccount.getSmtpUsername());

        // Decrypt password before using
        String decryptedPassword = EncryptionUtil.decrypt(emailAccount.getSmtpPassword());
        sender.setPassword(decryptedPassword);

        // Configure mail properties based on settings
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enabled", emailAccount.getUseTls());
        props.put("mail.smtp.starttls.required", emailAccount.getUseTls());
        props.put("mail.smtp.ssl.enable", emailAccount.getUseSsl());

        // Set socket factory protocol based on SSL/TLS
        if (emailAccount.getUseSsl()) {
            props.put("mail.smtp.socketFactory.protocol", "SSLv23");
            props.put("mail.smtp.socketFactory.port", emailAccount.getSmtpPort());
        } else {
            props.put("mail.smtp.socketFactory.protocol", "tcp");
        }

        // Set timeout properties for reliability
        props.put("mail.smtp.connectiontimeout", 10000); // 10 seconds
        props.put("mail.smtp.timeout", 10000); // 10 seconds
        props.put("mail.smtp.writetimeout", 10000); // 10 seconds

        log.debug("JavaMailSender configured for account: {} (host={}, port={}, useTls={}, useSsl={})",
                emailAccount.getName(), emailAccount.getSmtpHost(), emailAccount.getSmtpPort(),
                emailAccount.getUseTls(), emailAccount.getUseSsl());

        return sender;
    }

    /**
     * Extract a plain-text snippet from HTML content by stripping tags.
     */
    private String extractSnippet(String html, int maxLength) {
        if (html == null) return null;
        String text = html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
