package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.SendingMethod;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolder;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolderType;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailSettingGetterServices;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailStorageService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
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
    private final ObjectMapper objectMapper;

    @Autowired
    public EmailSendingService(
            EmailAccountRepository emailAccountRepository,
            EmailStorageService emailStorageService,
            EmailSettingGetterServices emailSettingGetterServices,
            EmailFolderRepository emailFolderRepository,
            EmailMessageRepository emailMessageRepository,
            ObjectMapper objectMapper) {
        this.emailAccountRepository = emailAccountRepository;
        this.emailStorageService = emailStorageService;
        this.emailSettingGetterServices = emailSettingGetterServices;
        this.emailFolderRepository = emailFolderRepository;
        this.emailMessageRepository = emailMessageRepository;
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

        // Capture sent email metadata asynchronously
        final EmailAccount captureAccount = emailAccount;
        CompletableFuture.runAsync(() -> captureResendSentEmail(captureAccount, resendEmailId, toEmail, subject));
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

        // Capture sent email asynchronously
        final EmailAccount captureAccount = emailAccount;
        final MimeMessage captureMessage = mimeMessage;
        CompletableFuture.runAsync(() -> captureSentEmail(captureAccount, captureMessage, toEmail, subject));
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

        // Capture sent email asynchronously (non-blocking)
        final EmailAccount captureAccount = emailAccount;
        final MimeMessage captureMessage = mimeMessage;
        CompletableFuture.runAsync(() -> captureSentEmail(captureAccount, captureMessage, toEmail, subject));
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
     * Runs asynchronously via CompletableFuture so it never blocks or fails the send.
     */
    private void captureSentEmail(EmailAccount account, MimeMessage mimeMessage, String toEmail, String subject) {
        try {
            if (!Boolean.TRUE.equals(emailSettingGetterServices.isSentCaptureEnabled())) {
                return;
            }

            Long accountId = account.getId();
            String messageId = mimeMessage.getMessageID();
            String fileName = emailStorageService.generateEmlFileName(messageId);

            // Save .eml to disk
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

            // Extract snippet from subject (system emails don't need full body parse)
            String snippet = subject != null && subject.length() > 200 ? subject.substring(0, 200) : subject;

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
                .hasAttachments(false)
                .attachmentCount(0)
                .fileName(fileName)
                .fileSize((long) baos.size())
                .storagePath("sent")
                .sentAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();

            emailMessageRepository.save(emailMessage);
            emailFolderRepository.incrementMessageCount(sentFolder.getId(), 1);

            log.debug("Captured sent email to {} in SENT folder for account {}", toEmail, account.getEmail());
        } catch (Exception e) {
            log.warn("Failed to capture sent email for account {}: {}", account.getEmail(), e.getMessage());
        }
    }

    /**
     * Capture a Resend-sent email metadata in the SENT folder.
     * Uses the Resend email ID to store metadata without .eml file.
     */
    private void captureResendSentEmail(EmailAccount account, String resendEmailId, String toEmail, String subject) {
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

            // Extract snippet from subject
            String snippet = subject != null && subject.length() > 200 ? subject.substring(0, 200) : subject;

            // Create metadata record using Resend email ID as messageId
            EmailMessage emailMessage = EmailMessage.builder()
                .emailAccount(account)
                .folder(sentFolder)
                .messageId(resendEmailId)
                .fromAddress(account.getEmail())
                .fromName(account.getName())
                .toAddresses(objectMapper.writeValueAsString(List.of(toEmail)))
                .subject(subject)
                .snippet(snippet)
                .isRead(true)
                .isDraft(false)
                .hasAttachments(false)
                .attachmentCount(0)
                .sentAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .build();

            emailMessageRepository.save(emailMessage);
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
}
