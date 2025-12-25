package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;

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

    @Autowired
    public EmailSendingService(EmailAccountRepository emailAccountRepository) {
        this.emailAccountRepository = emailAccountRepository;
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

            log.info("Using email account: {} ({}) to send email", emailAccount.getName(), emailAccount.getEmail());

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
