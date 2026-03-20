package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.CreateEmailAccountDTO;
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

import lombok.extern.slf4j.Slf4j;

/**
 * EmailAccountCreateService - Service for creating and validating new email accounts
 *
 * This service handles:
 * - Request validation
 * - Enum conversion for provider types
 * - SMTP password encryption using EncryptionUtil
 * - Duplicate email and name checks
 * - Entity creation and persistence
 * - Response formatting with ApiResponse
 */
@Service
@Slf4j
public class EmailAccountCreateService {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailAccountGetService emailAccountGetService;
    private final com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureCreateService emailAccountSignatureCreateService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );


    @Autowired
    public EmailAccountCreateService(
        EmailAccountRepository emailAccountRepository,
        IdObfuscator idObfuscator,
        EmailAccountGetService emailAccountGetService,
        com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureCreateService emailAccountSignatureCreateService
    ) {
        this.emailAccountRepository = emailAccountRepository;
        this.emailAccountGetService = emailAccountGetService;
        this.emailAccountSignatureCreateService = emailAccountSignatureCreateService;
    }

    /**
     * Create a new email account with validation and encryption
     *
     * @param createDTO The request DTO containing email account details
     * @return ResponseEntity with ApiResponse containing created account or error
     */
    @AuditLogAnnotation(
        action = "CREATE_EMAIL_ACCOUNT", 
        description = "Creating a new email account", 
        entityType = "EmailAccount"
    )
    public ResponseEntity<ApiResponse<?>> createEmailAccount(CreateEmailAccountDTO createDTO) {
        log.info("Creating new email account: {}", createDTO.getName());

        try {
            // Validate email format
            if (!EMAIL_PATTERN.matcher(createDTO.getEmail()).matches()) {
                log.warn("Invalid email format: {}", createDTO.getEmail());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid email format",
                        "INVALID_EMAIL_FORMAT"
                    )
                );
            }

            // Validate provider type
            EmailAccountProvider providerType = validateAndGetProviderType(createDTO.getProviderType());
            if (providerType == null) {
                log.warn("Invalid provider type: {}", createDTO.getProviderType());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400, 
                        "Invalid provider type", 
                        "INVALID_PROVIDER_TYPE"
                    )
                );
            }

            // Check for duplicate email
            if (emailAccountRepository.findByEmail(createDTO.getEmail()).isPresent()) {
                log.warn("Email already exists: {}", createDTO.getEmail());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400, 
                        "Email already exists", 
                        "DUPLICATE_EMAIL"
                    )
                );
            }

            // Check for duplicate name
            if (emailAccountRepository.findByName(createDTO.getName()).isPresent()) {
                log.warn("Account name already exists: {}", createDTO.getName());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Account name already exists",
                        "DUPLICATE_NAME"
                    )
                );
            }

            boolean isApiBasedProvider = providerType == EmailAccountProvider.RESEND;

            // Determine sending method
            SendingMethod sendingMethod = validateAndGetSendingMethod(createDTO.getSendingMethod());
            if (isApiBasedProvider && sendingMethod == null) {
                sendingMethod = SendingMethod.API; // Default to API for API-based providers
            } else if (!isApiBasedProvider) {
                sendingMethod = SendingMethod.SMTP; // Non-API providers always use SMTP
            } else if (sendingMethod == null) {
                sendingMethod = SendingMethod.SMTP;
            }

            // Determine if this account needs SMTP fields
            boolean needsSmtpFields = !isApiBasedProvider || sendingMethod == SendingMethod.SMTP;

            // Validate based on provider type and sending method
            if (isApiBasedProvider) {
                // API-based providers always require an API key (used as SMTP password too)
                if (createDTO.getApiKey() == null || createDTO.getApiKey().isBlank()) {
                    log.warn("API key is required for {} provider", providerType);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "API key is required for " + providerType.getDisplayName() + " provider", "API_KEY_REQUIRED")
                    );
                }

                if (sendingMethod == SendingMethod.SMTP) {
                    // Resend SMTP: port is required, host and username auto-filled
                    if (createDTO.getSmtpPort() == null) {
                        return ResponseEntity.badRequest().body(ApiResponse.error(400, "SMTP port is required for SMTP sending method", "SMTP_PORT_REQUIRED"));
                    }
                    // Validate SSL/TLS
                    Boolean useTls = createDTO.getUseTls() != null ? createDTO.getUseTls() : false;
                    Boolean useSsl = createDTO.getUseSsl() != null ? createDTO.getUseSsl() : true;
                    if (Boolean.TRUE.equals(useTls) && Boolean.TRUE.equals(useSsl)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Invalid SSL/TLS configuration: TLS and SSL cannot both be enabled.", "INVALID_SSL_TLS_CONFIGURATION")
                        );
                    }
                }
            } else {
                // SMTP-based providers require SMTP fields
                if (createDTO.getSmtpHost() == null || createDTO.getSmtpHost().isBlank()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, "SMTP host is required", "SMTP_HOST_REQUIRED"));
                }
                if (createDTO.getSmtpPort() == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, "SMTP port is required", "SMTP_PORT_REQUIRED"));
                }
                if (createDTO.getSmtpUsername() == null || createDTO.getSmtpUsername().isBlank()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, "SMTP username is required", "SMTP_USERNAME_REQUIRED"));
                }
                if (createDTO.getSmtpPassword() == null || createDTO.getSmtpPassword().isBlank()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, "SMTP password is required", "SMTP_PASSWORD_REQUIRED"));
                }
                if (createDTO.getUseTls() == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, "useTls is required", "USE_TLS_REQUIRED"));
                }
                if (createDTO.getUseSsl() == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400, "useSsl is required", "USE_SSL_REQUIRED"));
                }

                // Validate SSL/TLS configuration
                if (Boolean.TRUE.equals(createDTO.getUseTls()) && Boolean.TRUE.equals(createDTO.getUseSsl())) {
                    log.warn("Invalid SSL/TLS configuration: Both TLS and SSL cannot be enabled simultaneously");
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid SSL/TLS configuration: TLS and SSL cannot both be enabled. Please enable only one.", "INVALID_SSL_TLS_CONFIGURATION")
                    );
                }
            }

            // Encrypt sensitive credentials
            String encryptedPassword = !isApiBasedProvider && createDTO.getSmtpPassword() != null
                ? EncryptionUtil.encrypt(createDTO.getSmtpPassword()) : null;
            String encryptedApiKey = isApiBasedProvider && createDTO.getApiKey() != null
                ? EncryptionUtil.encrypt(createDTO.getApiKey()) : null;
            String encryptedWebhookSecret = createDTO.getWebhookSecret() != null && !createDTO.getWebhookSecret().isBlank()
                ? EncryptionUtil.encrypt(createDTO.getWebhookSecret()) : null;

            // Determine SMTP fields for API-based providers using SMTP sending method
            String smtpHost;
            Integer smtpPort;
            String smtpUsername;
            String smtpPassword;
            Boolean useTls;
            Boolean useSsl;

            if (isApiBasedProvider && sendingMethod == SendingMethod.SMTP) {
                // Resend SMTP: auto-fill host/username, use API key as password
                smtpHost = createDTO.getSmtpHost() != null && !createDTO.getSmtpHost().isBlank()
                    ? createDTO.getSmtpHost() : "smtp.resend.com";
                smtpPort = createDTO.getSmtpPort();
                smtpUsername = "resend";
                smtpPassword = encryptedApiKey; // API key is used as SMTP password
                useTls = createDTO.getUseTls() != null ? createDTO.getUseTls() : false;
                useSsl = createDTO.getUseSsl() != null ? createDTO.getUseSsl() : true;
            } else if (isApiBasedProvider) {
                // API sending: no SMTP fields needed
                smtpHost = null;
                smtpPort = null;
                smtpUsername = null;
                smtpPassword = null;
                useTls = null;
                useSsl = null;
            } else {
                // Standard SMTP provider
                smtpHost = createDTO.getSmtpHost();
                smtpPort = createDTO.getSmtpPort();
                smtpUsername = createDTO.getSmtpUsername();
                smtpPassword = encryptedPassword;
                useTls = createDTO.getUseTls();
                useSsl = createDTO.getUseSsl();
            }

            // Create email account entity
            EmailAccount emailAccount = EmailAccount.builder()
                .email(createDTO.getEmail())
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .smtpHost(smtpHost)
                .smtpPort(smtpPort)
                .smtpUsername(smtpUsername)
                .smtpPassword(smtpPassword)
                .useTls(useTls)
                .useSsl(useSsl)
                .enabled(false) // Enabled after testing
                .isDefault(false) // Enabled after testing
                .providerType(providerType)
                .sendingMethod(sendingMethod)
                .apiKey(encryptedApiKey)
                .webhookSecret(encryptedWebhookSecret)
                .rateLimitPerMinute(createDTO.getRateLimitPerMinute())
                .maxRetryAttempts(createDTO.getMaxRetryAttempts())
                .retryDelaySeconds(createDTO.getRetryDelaySeconds())
                .emailsSentCount(0L)
                .emailsFailedCount(0L)
                .receivingProtocol(validateAndGetReceivingProtocol(createDTO.getReceivingProtocol()))
                .imapHost(createDTO.getImapHost())
                .imapPort(createDTO.getImapPort())
                .imapUseSsl(createDTO.getImapUseSsl())
                .imapUseTls(createDTO.getImapUseTls())
                .receivingEnabled(false)
                .fetchIntervalMinutes(createDTO.getFetchIntervalMinutes() != null ? createDTO.getFetchIntervalMinutes() : 5)
                .maxFetchCount(createDTO.getMaxFetchCount() != null ? createDTO.getMaxFetchCount() : 50)
                .emailsReceivedCount(0L)
                .build();

            // Save to database
            EmailAccount savedAccount = emailAccountRepository.save(emailAccount);

            log.info("Email account created successfully with ID: {}", savedAccount.getId());

            // Create system default signature for the new email account
            try {
                boolean signatureCreated = emailAccountSignatureCreateService.createSystemDefaultSignature(savedAccount);
                if (signatureCreated) {
                    log.info("System default signature created for email account: {}", savedAccount.getId());
                } else {
                    log.warn("Failed to create system default signature for email account: {}", savedAccount.getId());
                }
            } catch (Exception e) {
                log.error("Error creating system default signature for email account: {}", savedAccount.getId(), e);
                // Don't fail the account creation if signature creation fails
            }

            // Create response with obfuscated ID
            EmailAccountDTO emailAccountDTO = emailAccountGetService.convertToDTO(emailAccount);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Email account created successfully",
                    emailAccountDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating email account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create email account", "EMAIL_ACCOUNT_CREATE_FAILED"));
        }
    }

    /**
     * Convert provider type integer to EmailAccountProvider enum
     *
     * @param providerTypeInt The provider type as integer
     * @return EmailAccountProvider enum or null if invalid
     */
    private ReceivingProtocol validateAndGetReceivingProtocol(Integer protocolInt) {
        if (protocolInt == null) return ReceivingProtocol.NONE;
        return switch (protocolInt) {
            case 1 -> ReceivingProtocol.IMAP;
            case 2 -> ReceivingProtocol.POP3;
            default -> ReceivingProtocol.NONE;
        };
    }

    private EmailAccountProvider validateAndGetProviderType(Integer providerTypeInt) {
        if (providerTypeInt == null) {
            return null;
        }

        switch (providerTypeInt) {
            case 1:
                return EmailAccountProvider.GMAIL;
            case 2:
                return EmailAccountProvider.OUTLOOK;
            case 3:
                return EmailAccountProvider.SENDGRID;
            case 4:
                return EmailAccountProvider.MAILGUN;
            case 5:
                return EmailAccountProvider.AWS_SES;
            case 6:
                return EmailAccountProvider.CUSTOM;
            case 7:
                return EmailAccountProvider.RESEND;
            default:
                return null;
        }
    }

    private SendingMethod validateAndGetSendingMethod(Integer sendingMethodInt) {
        if (sendingMethodInt == null) return null;
        return switch (sendingMethodInt) {
            case 1 -> SendingMethod.API;
            case 2 -> SendingMethod.SMTP;
            default -> null;
        };
    }
}
