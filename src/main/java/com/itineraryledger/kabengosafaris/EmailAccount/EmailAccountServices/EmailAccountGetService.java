package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.EmailAccountDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailAccountGetService - Service for retrieving and filtering email accounts with pagination
 *
 * This service provides methods to fetch email accounts with:
 * - Specification-based dynamic filtering
 * - Pagination and sorting support
 * - Obfuscated ID conversion for DTOs
 */
@Service
@Slf4j
public class EmailAccountGetService {

    private final EmailAccountRepository emailAccountRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public EmailAccountGetService(EmailAccountRepository emailAccountRepository, IdObfuscator idObfuscator) {
        this.emailAccountRepository = emailAccountRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all email accounts with pagination, filtering, and sorting
     * Controller-style method for API endpoints
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param enabled Filter by enabled status
     * @param isDefault Filter by default account status
     * @param email Filter by email address (partial match)
     * @param name Filter by account name (partial match)
     * @param providerTypeStr Filter by provider type (as string)
     * @param smtpHost Filter by SMTP host (partial match)
     * @param hasErrors Filter by error status
     * @param sortDir Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    public ResponseEntity<?> getAllEmailAccounts(
        int page,
        int size,
        Boolean enabled,
        Boolean isDefault,
        String email,
        String name,
        int providerTypeLong,
        String smtpHost,
        Boolean hasErrors,
        String sortDir
    ) {

        log.debug("Fetching email accounts with filters - page: {}, size: {}, enabled: {}, isDefault: {}, " +
                "email: {}, name: {}, providerType: {}, smtpHost: {}, hasErrors: {}, sortDir: {}",
                page, size, enabled, isDefault, email, name, providerTypeLong, smtpHost, hasErrors, sortDir);

        // Validate pagination parameters
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            log.warn("Invalid page size: {}", size);
            return ResponseEntity.badRequest().body("Page size must be greater than 0");
        }

        // Parse provider type enum if provided
        EmailAccountProvider providerType = null;

        switch (providerTypeLong) {
            case 1:
                providerType = EmailAccountProvider.GMAIL;
                break;
            case 2:
                providerType = EmailAccountProvider.OUTLOOK;
                break;
            case 3:
                providerType = EmailAccountProvider.SENDGRID;
                break;
            case 4:
                providerType = EmailAccountProvider.MAILGUN;
                break;
            case 5:
                providerType = EmailAccountProvider.AWS_SES;
                break;
            case 6:
                providerType = EmailAccountProvider.CUSTOM;
                break;
            default:
                providerType = null;
                break;
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDir)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(
            page,
            size,
            Sort.by(direction, "createdAt")
        );

        // Build dynamic specification
        Specification<EmailAccount> specification = Specification.unrestricted();

        if (enabled != null) {
            specification = specification.and(EmailAccountSpecification.isEnabled(enabled));
        }

        if (isDefault != null) {
            specification = specification.and(EmailAccountSpecification.isDefault(isDefault));
        }

        if (email != null && !email.isBlank()) {
            specification = specification.and(EmailAccountSpecification.emailLike(email));
        }

        if (name != null && !name.isBlank()) {
            specification = specification.and(EmailAccountSpecification.nameLike(name));
        }

        if (providerType != null) {
            specification = specification.and(EmailAccountSpecification.providerType(providerType));
        }

        if (smtpHost != null && !smtpHost.isBlank()) {
            specification = specification.and(EmailAccountSpecification.smtpHostLike(smtpHost));
        }

        if (hasErrors != null && hasErrors) {
            specification = specification.and(EmailAccountSpecification.hasErrors());
        }

        // Execute query with specifications
        Page<EmailAccount> pagedEmailAccounts = emailAccountRepository.findAll(specification, paging);

        // Convert to DTOs
        List<EmailAccountDTO> emailAccountDTOs = getEmailAccountDTOs(pagedEmailAccounts.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("emailAccounts", emailAccountDTOs);
        response.put("currentPage", pagedEmailAccounts.getNumber());
        response.put("totalItems", pagedEmailAccounts.getTotalElements());
        response.put("totalPages", pagedEmailAccounts.getTotalPages());

        log.info("Successfully fetched {} email accounts on page {}", emailAccountDTOs.size(), page);
        return ResponseEntity.ok(
            ApiResponse.success(
                200, 
                "Successfully retrieved email accounts.",
                response
            )
        );
    }

    public ResponseEntity<ApiResponse<?>> getEmailAccount(String idObfuscated) {
        try {
            // Decode obfuscated ID
            Long id = idObfuscator.decodeId(idObfuscated);
            
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

            return ResponseEntity.ok(
                ApiResponse.success(
                    200, 
                    "Successfully retrieved email accounts.",
                    convertToDTO(emailAccount)
                )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            500,
                            "Failed to get email account",
                            "GET_EMAIL_ACCOUNT_FAILED"
                    )
            );
        }
    }


    /**
     * Convert list of EmailAccount entities to EmailAccountDTOs with obfuscated IDs
     *
     * @param emailAccounts The entities to convert
     * @return List of EmailAccountDTO with obfuscated IDs
     */
    private List<EmailAccountDTO> getEmailAccountDTOs(List<EmailAccount> emailAccounts) {
        return emailAccounts.stream().map(this::convertToDTO).toList();
    }

    /**
     * Convert EmailAccount entity to EmailAccountDTO with obfuscated ID
     *
     * @param emailAccount The entity to convert
     * @return EmailAccountDTO with obfuscated ID
     */
    public EmailAccountDTO convertToDTO(EmailAccount emailAccount) {
        return EmailAccountDTO.builder()
                .id(idObfuscator.encodeId(emailAccount.getId()))
                .email(emailAccount.getEmail())
                .name(emailAccount.getName())
                .description(emailAccount.getDescription())
                .smtpHost(emailAccount.getSmtpHost())
                .smtpPort(emailAccount.getSmtpPort())
                .smtpUsername(emailAccount.getSmtpUsername())
                .useTls(emailAccount.getUseTls())
                .useSsl(emailAccount.getUseSsl())
                .enabled(emailAccount.getEnabled())
                .isDefault(emailAccount.getIsDefault())
                .providerType(emailAccount.getProviderType())
                .rateLimitPerMinute(emailAccount.getRateLimitPerMinute())
                .maxRetryAttempts(emailAccount.getMaxRetryAttempts())
                .retryDelaySeconds(emailAccount.getRetryDelaySeconds())
                .lastTestedAt(emailAccount.getLastTestedAt())
                .lastErrorMessage(emailAccount.getLastErrorMessage())
                .emailsSentCount(emailAccount.getEmailsSentCount())
                .emailsFailedCount(emailAccount.getEmailsFailedCount())
                .createdAt(emailAccount.getCreatedAt())
                .updatedAt(emailAccount.getUpdatedAt())
                .createdBy(emailAccount.getCreatedBy())
                .updatedBy(emailAccount.getUpdatedBy())
                .build();
    }
}
