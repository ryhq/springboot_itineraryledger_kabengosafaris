package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.time.LocalDateTime;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.EmailAccountDTO;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailAccountTestService - Service for testing email account SMTP connections
 *
 * This service handles:
 * - Testing SMTP connectivity with retry logic
 * - Respecting maxRetryAttempts and retryDelaySeconds settings
 * - Recording test results (lastTestedAt, lastErrorMessage)
 * - Automatically enabling account on successful test
 * - Validating rate limiting configuration
 * - Creating and configuring JavaMailSender instances
 */
@Service
@Slf4j
@Transactional
public class EmailAccountTestService {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailAccountGetService emailAccountGetService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public EmailAccountTestService(
            EmailAccountRepository emailAccountRepository,
            EmailAccountGetService emailAccountGetService,
            IdObfuscator idObfuscator) {
        this.emailAccountRepository = emailAccountRepository;
        this.emailAccountGetService = emailAccountGetService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Test email account SMTP connection with retry logic
     *
     * @param idObfuscated The obfuscated email account ID
     * @return ResponseEntity with ApiResponse containing test result and updated account
     */
    public ResponseEntity<ApiResponse<?>> testEmailAccount(String idObfuscated) {
        log.info("Testing email account with ID: {}", idObfuscated);
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);
            
            return testEmailAccount(id);

        } catch (Exception e) {
            log.error("Error testing email account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500, 
                    "Failed to test email account", 
                    "EMAIL_ACCOUNT_TEST_FAILED"
                )
            );
        }
    }

    @AuditLogAnnotation(action = "TEST_EMAIL_ACCOUNT", description = "Testing email account SMTP connection", entityType = "EmailAccount", entityIdParamName = "id")
    private ResponseEntity<ApiResponse<?>> testEmailAccount(Long id) {
        EmailAccount emailAccount = emailAccountRepository.findById(id).orElse(null);

        if (emailAccount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                    404,
                    "Email account not found",
                    "EMAIL_ACCOUNT_NOT_FOUND"
                )
            );
        }

        if (Boolean.TRUE.equals(emailAccount.getEnabled())) {
            log.warn("Cannot test connection for already enabled account: {}", id);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Email account is already enabled and tested. Disable the account first to re-test the connection.",
                    "ACCOUNT_ALREADY_TESTED"
                )
            );
        }

        log.info("Starting SMTP connection test for account: {} ({})", emailAccount.getName(), id);

        // Perform test with retry logic
        boolean testPassed = testConnectionWithRetry(emailAccount);

        if (testPassed) {
            // Update account on successful test
            emailAccount.setEnabled(true);
            emailAccount.setLastTestedAt(LocalDateTime.now());
            emailAccount.setLastErrorMessage(null);

            log.info("SMTP test passed for account: {}", emailAccount.getName());
        } else {
            // Keep last error message from retry attempts
            emailAccount.setLastTestedAt(LocalDateTime.now());
            // lastErrorMessage already set by retry logic
            emailAccount.setEnabled(false);

            log.warn("SMTP test failed for account: {} - {}", emailAccount.getName(), emailAccount.getLastErrorMessage());
        }

        // Save updated account
        EmailAccount updated = emailAccountRepository.save(emailAccount);

        // Build response
        EmailAccountDTO emailAccountDTO = emailAccountGetService.convertToDTO(updated);

        if (testPassed) {
            log.info("SMTP test passed and account enabled for: {}", emailAccount.getName());
            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Email account connection test passed successfully. Account enabled.",
                    emailAccountDTO
                )
            );
        } else {
            log.error("SMTP test failed for account: {} - {}", emailAccount.getName(), emailAccount.getLastErrorMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Email account connection test failed. " + emailAccount.getLastErrorMessage(),
                    "SMTP_TEST_FAILED"
                )
            );
        }
    }

    /**
     * Test SMTP connection with retry logic
     * Respects maxRetryAttempts and retryDelaySeconds settings
     * Saves error messages to database immediately on failure
     *
     * @param emailAccount The email account to test
     * @return true if connection successful, false otherwise
     */
    private boolean testConnectionWithRetry(EmailAccount emailAccount) {
        int maxRetries = emailAccount.getMaxRetryAttempts() != null ? emailAccount.getMaxRetryAttempts() : 3;
        int retryDelaySeconds = emailAccount.getRetryDelaySeconds() != null ? emailAccount.getRetryDelaySeconds() : 5;

        log.debug("Starting retry loop: maxRetries={}, delaySeconds={}", maxRetries, retryDelaySeconds);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("SMTP test attempt {}/{} for account: {}", attempt, maxRetries, emailAccount.getName());

                // Create mail sender with account configuration
                JavaMailSender mailSender = createMailSender(emailAccount);

                // Send test email with display name
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                // Set from address with display name
                helper.setFrom(emailAccount.getEmail(), emailAccount.getName());
                helper.setTo(emailAccount.getEmail()); // Send to self for testing
                helper.setSubject("Test Email - Do Not Reply");
                helper.setText("This is a test email to verify SMTP configuration for: " + emailAccount.getName() + "\n\nTimestamp: " + LocalDateTime.now());

                mailSender.send(mimeMessage);

                log.info("SMTP test succeeded on attempt {}/{} for account: {}", attempt, maxRetries, emailAccount.getName());
                return true;

            } catch (RuntimeException e) {
                // Fail fast for configuration/decryption errors - these won't be fixed by retrying
                if (e.getMessage() != null && e.getMessage().contains("Decryption failed")) {
                    log.error("Decryption failed for account password. Account configuration may be corrupted: {}", emailAccount.getName(), e);
                    emailAccount.setLastErrorMessage("Decryption failed: Unable to decrypt SMTP password. Account configuration may be corrupted.");
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                    return false;
                }

                log.warn("SMTP test attempt {}/{} failed for account: {} - {}", attempt, maxRetries, emailAccount.getName(), e.getMessage());

                // Store error message for the last attempt
                if (attempt == maxRetries) {
                    emailAccount.setLastErrorMessage("SMTP test failed after " + maxRetries + " attempts: " + e.getMessage());
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                    log.error("All retry attempts exhausted for account: {}", emailAccount.getName(), e);
                } else if (attempt < maxRetries) {
                    // Wait before retrying
                    try {
                        log.debug("Waiting {} seconds before retry attempt {}", retryDelaySeconds, attempt + 1);
                        Thread.sleep(retryDelaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        emailAccount.setLastErrorMessage("SMTP test interrupted: " + ie.getMessage());
                        emailAccount.setLastTestedAt(LocalDateTime.now());
                        emailAccountRepository.save(emailAccount);
                        log.error("Test interrupted for account: {}", emailAccount.getName(), ie);
                        return false;
                    }
                }
            } catch (Exception e) {
                log.warn("SMTP test attempt {}/{} failed for account: {} - {}", attempt, maxRetries, emailAccount.getName(), e.getMessage());

                // Store error message for the last attempt
                if (attempt == maxRetries) {
                    emailAccount.setLastErrorMessage("SMTP test failed after " + maxRetries + " attempts: " + e.getMessage());
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                    log.error("All retry attempts exhausted for account: {}", emailAccount.getName(), e);
                } else if (attempt < maxRetries) {
                    // Wait before retrying
                    try {
                        log.debug("Waiting {} seconds before retry attempt {}", retryDelaySeconds, attempt + 1);
                        Thread.sleep(retryDelaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        emailAccount.setLastErrorMessage("SMTP test interrupted: " + ie.getMessage());
                        emailAccount.setLastTestedAt(LocalDateTime.now());
                        emailAccountRepository.save(emailAccount);
                        log.error("Test interrupted for account: {}", emailAccount.getName(), ie);
                        return false;
                    }
                }
            }
        }

        return false;
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