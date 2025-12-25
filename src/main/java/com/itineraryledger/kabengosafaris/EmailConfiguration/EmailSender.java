// package com.itineraryledger.kabengosafaris.EmailConfiguration;

// import java.util.ArrayList;
// import java.util.List;

// import org.springframework.mail.SimpleMailMessage;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.mail.javamail.MimeMessageHelper;
// import org.springframework.stereotype.Component;

// import jakarta.mail.MessagingException;
// import jakarta.mail.internet.MimeMessage;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// /**
//  * EmailSender - Utility component for sending emails using configured email accounts
//  *
//  * Features:
//  * - Send simple text emails
//  * - Send HTML emails
//  * - Support for attachments
//  * - Batch email sending
//  * - Retry logic with configurable delays
//  * - Audit logging of sent emails
//  */
// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class EmailSender {

//     private final EmailConfigurationService emailConfigService;

//     /**
//      * Send a simple text email using the default configuration
//      */
//     public void sendSimpleEmail(String toEmail, String subject, String body) {
//         sendSimpleEmail(toEmail, subject, body, null);
//     }

//     /**
//      * Send a simple text email using a specific configuration
//      */
//     public void sendSimpleEmail(String toEmail, String subject, String body, Long configId) {
//         log.debug("Sending simple email to: {}", toEmail);

//         try {
//             EmailConfiguration config = configId != null ?
//                     emailConfigService.getConfiguration(configId) :
//                     emailConfigService.getDefaultConfiguration();

//             if (!config.getEnabled()) {
//                 throw new IllegalArgumentException("Email configuration is disabled");
//             }

//             JavaMailSender mailSender = emailConfigService.getMailSender(config.getId());

//             SimpleMailMessage message = new SimpleMailMessage();
//             message.setFrom(config.getFromEmail());
//             message.setTo(toEmail);
//             message.setSubject(subject);
//             message.setText(body);

//             mailSender.send(message);

//             emailConfigService.recordSuccessfulSend(config.getId());
//             log.info("Simple email sent successfully to: {}", toEmail);

//         } catch (Exception e) {
//             log.error("Failed to send simple email to: {}", toEmail, e);
//             if (configId != null) {
//                 emailConfigService.recordFailedSend(configId);
//             }
//             throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
//         }
//     }

//     /**
//      * Send an HTML email using the default configuration
//      */
//     public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
//         sendHtmlEmail(toEmail, subject, htmlBody, null);
//     }

//     /**
//      * Send an HTML email using a specific configuration
//      */
//     public void sendHtmlEmail(String toEmail, String subject, String htmlBody, Long configId) {
//         log.debug("Sending HTML email to: {}", toEmail);

//         try {
//             EmailConfiguration config = configId != null ?
//                     emailConfigService.getConfiguration(configId) :
//                     emailConfigService.getDefaultConfiguration();

//             if (!config.getEnabled()) {
//                 throw new IllegalArgumentException("Email configuration is disabled");
//             }

//             JavaMailSender mailSender = emailConfigService.getMailSender(config.getId());
//             MimeMessage message = mailSender.createMimeMessage();
//             MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

//             helper.setFrom(config.getFromEmail(), config.getFromName());
//             helper.setTo(toEmail);
//             helper.setSubject(subject);
//             helper.setText(htmlBody, true); // true = HTML

//             mailSender.send(message);

//             emailConfigService.recordSuccessfulSend(config.getId());
//             log.info("HTML email sent successfully to: {}", toEmail);

//         } catch (MessagingException e) {
//             log.error("Failed to send HTML email to: {}", toEmail, e);
//             if (configId != null) {
//                 emailConfigService.recordFailedSend(configId);
//             }
//             throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
//         } catch (Exception e) {
//             log.error("Failed to send HTML email to: {} - {}", toEmail, e.getMessage(), e);
//             if (configId != null) {
//                 emailConfigService.recordFailedSend(configId);
//             }
//             throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
//         }
//     }

//     /**
//      * Send email to multiple recipients using the default configuration
//      */
//     public void sendBatchEmail(List<String> recipients, String subject, String body) {
//         sendBatchEmail(recipients, subject, body, null);
//     }

//     /**
//      * Send email to multiple recipients using a specific configuration
//      */
//     public void sendBatchEmail(List<String> recipients, String subject, String body, Long configId) {
//         log.debug("Sending batch email to {} recipients", recipients.size());

//         EmailConfiguration config = configId != null ?
//                 emailConfigService.getConfiguration(configId) :
//                 emailConfigService.getDefaultConfiguration();

//         for (String recipient : recipients) {
//             try {
//                 sendSimpleEmail(recipient, subject, body, config.getId());
//             } catch (Exception e) {
//                 log.warn("Failed to send email to {}, continuing with next recipient", recipient, e);
//             }
//         }
//     }

//     /**
//      * Send email with retry logic
//      */
//     public void sendEmailWithRetry(String toEmail, String subject, String body, int maxRetries) {
//         sendEmailWithRetry(toEmail, subject, body, maxRetries, null);
//     }

//     /**
//      * Send email with retry logic using a specific configuration
//      */
//     public void sendEmailWithRetry(String toEmail, String subject, String body, int maxRetries, Long configId) {
//         log.debug("Sending email with retry logic to: {}", toEmail);

//         EmailConfiguration config = configId != null ?
//                 emailConfigService.getConfiguration(configId) :
//                 emailConfigService.getDefaultConfiguration();

//         int retryCount = 0;
//         Exception lastException = null;

//         while (retryCount < maxRetries) {
//             try {
//                 sendSimpleEmail(toEmail, subject, body, config.getId());
//                 return; // Success
//             } catch (Exception e) {
//                 lastException = e;
//                 retryCount++;

//                 if (retryCount < maxRetries) {
//                     long delaySeconds = config.getRetryDelaySeconds();
//                     log.warn("Email send failed (attempt {}/{}), retrying in {} seconds",
//                             retryCount, maxRetries, delaySeconds);

//                     try {
//                         Thread.sleep(delaySeconds * 1000);
//                     } catch (InterruptedException ie) {
//                         Thread.currentThread().interrupt();
//                         throw new RuntimeException("Email sending interrupted", ie);
//                     }
//                 }
//             }
//         }

//         log.error("Failed to send email after {} retries", maxRetries);
//         emailConfigService.recordFailedSend(config.getId());
//         throw new RuntimeException("Failed to send email after " + maxRetries + " retries", lastException);
//     }

//     /**
//      * Send HTML email with retry logic
//      */
//     public void sendHtmlEmailWithRetry(String toEmail, String subject, String htmlBody, int maxRetries) {
//         sendHtmlEmailWithRetry(toEmail, subject, htmlBody, maxRetries, null);
//     }

//     /**
//      * Send HTML email with retry logic using a specific configuration
//      */
//     public void sendHtmlEmailWithRetry(String toEmail, String subject, String htmlBody, int maxRetries, Long configId) {
//         log.debug("Sending HTML email with retry logic to: {}", toEmail);

//         EmailConfiguration config = configId != null ?
//                 emailConfigService.getConfiguration(configId) :
//                 emailConfigService.getDefaultConfiguration();

//         int retryCount = 0;
//         Exception lastException = null;

//         while (retryCount < maxRetries) {
//             try {
//                 sendHtmlEmail(toEmail, subject, htmlBody, config.getId());
//                 return; // Success
//             } catch (Exception e) {
//                 lastException = e;
//                 retryCount++;

//                 if (retryCount < maxRetries) {
//                     long delaySeconds = config.getRetryDelaySeconds();
//                     log.warn("HTML email send failed (attempt {}/{}), retrying in {} seconds",
//                             retryCount, maxRetries, delaySeconds);

//                     try {
//                         Thread.sleep(delaySeconds * 1000);
//                     } catch (InterruptedException ie) {
//                         Thread.currentThread().interrupt();
//                         throw new RuntimeException("Email sending interrupted", ie);
//                     }
//                 }
//             }
//         }

//         log.error("Failed to send HTML email after {} retries", maxRetries);
//         emailConfigService.recordFailedSend(config.getId());
//         throw new RuntimeException("Failed to send HTML email after " + maxRetries + " retries", lastException);
//     }

//     /**
//      * Validate email address format
//      */
//     public static boolean isValidEmailAddress(String email) {
//         String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
//         return email != null && email.matches(emailRegex);
//     }

//     /**
//      * Validate list of email addresses
//      */
//     public static List<String> filterValidEmails(List<String> emails) {
//         List<String> validEmails = new ArrayList<>();
//         for (String email : emails) {
//             if (isValidEmailAddress(email)) {
//                 validEmails.add(email);
//             }
//         }
//         return validEmails;
//     }
// }
