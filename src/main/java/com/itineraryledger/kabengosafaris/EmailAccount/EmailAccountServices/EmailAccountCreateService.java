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
    @AuditLogAnnotation(action = "ADD_EMAIL_ACCOUNT", description = "Creating a new email account", entityType = "EmailAccount")
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

            // Validate SSL/TLS configuration
            if (Boolean.TRUE.equals(createDTO.getUseTls()) && Boolean.TRUE.equals(createDTO.getUseSsl())) {
                log.warn("Invalid SSL/TLS configuration: Both TLS and SSL cannot be enabled simultaneously");
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid SSL/TLS configuration: TLS and SSL cannot both be enabled. Please enable only one.",
                        "INVALID_SSL_TLS_CONFIGURATION"
                    )
                );
            }

            // Encrypt SMTP password
            String encryptedPassword = EncryptionUtil.encrypt(createDTO.getSmtpPassword());

            // Create email account entity
            EmailAccount emailAccount = EmailAccount.builder()
                .email(createDTO.getEmail())
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .smtpHost(createDTO.getSmtpHost())
                .smtpPort(createDTO.getSmtpPort())
                .smtpUsername(createDTO.getSmtpUsername())
                .smtpPassword(encryptedPassword)
                .useTls(createDTO.getUseTls())
                .useSsl(createDTO.getUseSsl())
                .enabled(false) // Enabled after testing
                .isDefault(false) // Enabled after testing 
                .providerType(providerType)
                .rateLimitPerMinute(createDTO.getRateLimitPerMinute())
                .maxRetryAttempts(createDTO.getMaxRetryAttempts())
                .retryDelaySeconds(createDTO.getRetryDelaySeconds())
                .emailsSentCount(0L)
                .emailsFailedCount(0L)
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
            default:
                return null;
        }
    }
}
