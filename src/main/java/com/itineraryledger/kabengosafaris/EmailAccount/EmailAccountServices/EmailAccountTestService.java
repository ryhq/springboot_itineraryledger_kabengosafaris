package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.time.LocalDateTime;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.EmailAccountDTO;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ReceivingProtocol;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.SendingMethod;
import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

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
    @AuditLogAnnotation(action = "TEST_EMAIL_ACCOUNT", description = "Testing email account SMTP connection", entityType = "EmailAccount", entityIdParamName = "idObfuscated")
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

        log.info("Starting connection test for account: {} ({}) [{}]", emailAccount.getName(), id, emailAccount.getProviderType());

        // Perform test with retry logic — route based on provider and sending method
        boolean testPassed;
        if (emailAccount.getProviderType() == EmailAccountProvider.RESEND
                && emailAccount.getSendingMethod() == SendingMethod.API) {
            testPassed = testResendConnection(emailAccount);
        } else if (emailAccount.getProviderType() == EmailAccountProvider.RESEND
                && emailAccount.getSendingMethod() == SendingMethod.SMTP) {
            testPassed = testResendSmtpConnection(emailAccount);
        } else {
            testPassed = testConnectionWithRetry(emailAccount);
        }

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
     * Test Resend API connection by sending a test email
     */
    private boolean testResendConnection(EmailAccount emailAccount) {
        int maxRetries = emailAccount.getMaxRetryAttempts() != null ? emailAccount.getMaxRetryAttempts() : 3;
        int retryDelaySeconds = emailAccount.getRetryDelaySeconds() != null ? emailAccount.getRetryDelaySeconds() : 5;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Resend test attempt {}/{} for account: {}", attempt, maxRetries, emailAccount.getName());

                if (emailAccount.getApiKey() == null || emailAccount.getApiKey().isBlank()) {
                    emailAccount.setLastErrorMessage("Resend test failed: API key is not configured");
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                    return false;
                }

                String apiKey = EncryptionUtil.decrypt(emailAccount.getApiKey());
                Resend resend = new Resend(apiKey);

                CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(emailAccount.getName() + " <" + emailAccount.getEmail() + ">")
                    .to(emailAccount.getEmail()) // Send to self for testing
                    .subject("Test Email - Do Not Reply")
                    .html("<p>This is a test email to verify Resend API configuration for: " + emailAccount.getName() + "</p><p>Timestamp: " + LocalDateTime.now() + "</p>")
                    .build();

                CreateEmailResponse response = resend.emails().send(params);
                log.info("Resend test succeeded on attempt {}/{} for account: {}. Email ID: {}", attempt, maxRetries, emailAccount.getName(), response.getId());
                return true;

            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("Decryption failed")) {
                    emailAccount.setLastErrorMessage("Decryption failed: Unable to decrypt API key. Account configuration may be corrupted.");
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                    return false;
                }

                log.warn("Resend test attempt {}/{} failed for account: {} - {}", attempt, maxRetries, emailAccount.getName(), e.getMessage());
                if (attempt == maxRetries) {
                    emailAccount.setLastErrorMessage("Resend test failed after " + maxRetries + " attempts: " + e.getMessage());
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                } else {
                    try {
                        Thread.sleep(retryDelaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        emailAccount.setLastErrorMessage("Resend test interrupted: " + ie.getMessage());
                        emailAccount.setLastTestedAt(LocalDateTime.now());
                        emailAccountRepository.save(emailAccount);
                        return false;
                    }
                }
            } catch (Exception e) {
                log.warn("Resend test attempt {}/{} failed for account: {} - {}", attempt, maxRetries, emailAccount.getName(), e.getMessage());
                if (attempt == maxRetries) {
                    emailAccount.setLastErrorMessage("Resend test failed after " + maxRetries + " attempts: " + e.getMessage());
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                } else {
                    try {
                        Thread.sleep(retryDelaySeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        emailAccount.setLastErrorMessage("Resend test interrupted: " + ie.getMessage());
                        emailAccount.setLastTestedAt(LocalDateTime.now());
                        emailAccountRepository.save(emailAccount);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Test Resend SMTP gateway connection by sending a test email via smtp.resend.com
     */
    private boolean testResendSmtpConnection(EmailAccount emailAccount) {
        int maxRetries = emailAccount.getMaxRetryAttempts() != null ? emailAccount.getMaxRetryAttempts() : 3;
        int retryDelaySeconds = emailAccount.getRetryDelaySeconds() != null ? emailAccount.getRetryDelaySeconds() : 5;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.debug("Resend SMTP test attempt {}/{} for account: {}", attempt, maxRetries, emailAccount.getName());

                if (emailAccount.getApiKey() == null || emailAccount.getApiKey().isBlank()) {
                    emailAccount.setLastErrorMessage("Resend SMTP test failed: API key is not configured (used as SMTP password)");
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                    return false;
                }

                // Build JavaMailSender for Resend SMTP
                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(emailAccount.getSmtpHost() != null ? emailAccount.getSmtpHost() : "smtp.resend.com");
                sender.setPort(emailAccount.getSmtpPort() != null ? emailAccount.getSmtpPort() : 465);
                sender.setUsername("resend");

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

                MimeMessage mimeMessage = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(emailAccount.getEmail(), emailAccount.getName());
                helper.setTo(emailAccount.getEmail());
                helper.setSubject("Test Email - Do Not Reply");
                helper.setText("<p>This is a test email to verify Resend SMTP configuration for: " + emailAccount.getName() + "</p><p>Timestamp: " + LocalDateTime.now() + "</p>", true);

                sender.send(mimeMessage);
                log.info("Resend SMTP test succeeded on attempt {}/{} for account: {}", attempt, maxRetries, emailAccount.getName());
                return true;

            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("Decryption failed")) {
                    emailAccount.setLastErrorMessage("Decryption failed: Unable to decrypt API key for SMTP password.");
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                    return false;
                }

                log.warn("Resend SMTP test attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    emailAccount.setLastErrorMessage("Resend SMTP test failed after " + maxRetries + " attempts: " + e.getMessage());
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                } else {
                    try { Thread.sleep(retryDelaySeconds * 1000L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
                }
            } catch (Exception e) {
                log.warn("Resend SMTP test attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    emailAccount.setLastErrorMessage("Resend SMTP test failed after " + maxRetries + " attempts: " + e.getMessage());
                    emailAccount.setLastTestedAt(LocalDateTime.now());
                    emailAccountRepository.save(emailAccount);
                } else {
                    try { Thread.sleep(retryDelaySeconds * 1000L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return false; }
                }
            }
        }
        return false;
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

    /**
     * Test IMAP/POP3 connection for an email account
     */
    @AuditLogAnnotation(action = "TEST_IMAP_CONNECTION", description = "Testing email account IMAP/POP3 connection", entityType = "EmailAccount", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> testImapConnection(String idObfuscated) {
        log.info("Testing IMAP connection for account ID: {}", idObfuscated);
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            EmailAccount account = emailAccountRepository.findById(id).orElse(null);

            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            if (account.getReceivingProtocol() == null || account.getReceivingProtocol() == ReceivingProtocol.NONE) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No receiving protocol configured for this account", "NO_RECEIVING_PROTOCOL"));
            }

            if (account.getImapHost() == null || account.getImapPort() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "IMAP/POP3 host and port must be configured", "MISSING_IMAP_CONFIG"));
            }

            Store store = null;
            try {
                Properties props = new Properties();
                String protocol;

                if (account.getReceivingProtocol() == ReceivingProtocol.IMAP) {
                    protocol = Boolean.TRUE.equals(account.getImapUseSsl()) ? "imaps" : "imap";
                } else {
                    protocol = Boolean.TRUE.equals(account.getImapUseSsl()) ? "pop3s" : "pop3";
                }

                props.put("mail.store.protocol", protocol);
                props.put("mail." + protocol + ".host", account.getImapHost());
                props.put("mail." + protocol + ".port", String.valueOf(account.getImapPort()));
                if (Boolean.TRUE.equals(account.getImapUseTls())) {
                    props.put("mail." + protocol + ".starttls.enable", "true");
                }
                props.put("mail." + protocol + ".connectiontimeout", "10000");
                props.put("mail." + protocol + ".timeout", "10000");

                Session session = Session.getInstance(props);
                store = session.getStore(protocol);
                String decryptedPassword = EncryptionUtil.decrypt(account.getSmtpPassword());
                store.connect(account.getImapHost(), account.getSmtpUsername(), decryptedPassword);

                // Update account
                account.setLastFetchErrorMessage(null);
                emailAccountRepository.save(account);

                log.info("IMAP/POP3 test passed for account: {}", account.getEmail());

                EmailAccountDTO dto = emailAccountGetService.convertToDTO(account);
                return ResponseEntity.ok(ApiResponse.success(200,
                    account.getReceivingProtocol().getDisplayName() + " connection test passed successfully", dto));

            } catch (Exception e) {
                account.setLastFetchErrorMessage(e.getMessage());
                emailAccountRepository.save(account);

                log.error("IMAP/POP3 test failed for account {}: {}", account.getEmail(), e.getMessage());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        account.getReceivingProtocol().getDisplayName() + " connection test failed: " + e.getMessage(),
                        "IMAP_TEST_FAILED"));
            } finally {
                if (store != null && store.isConnected()) {
                    try { store.close(); } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            log.error("Error testing IMAP connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to test IMAP connection", "IMAP_TEST_ERROR"));
        }
    }
}