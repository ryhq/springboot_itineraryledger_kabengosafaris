package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices;

import java.util.Arrays;
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
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "email", "name", "providerType", "enabled", "isDefault", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public EmailAccountGetService(
        EmailAccountRepository emailAccountRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.emailAccountRepository = emailAccountRepository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    /** 1..7 on the wire, because that is what this module chose. */
    private EmailAccountProvider providerFromInt(int value) {
        return switch (value) {
            case 1 -> EmailAccountProvider.GMAIL;
            case 2 -> EmailAccountProvider.OUTLOOK;
            case 3 -> EmailAccountProvider.SENDGRID;
            case 4 -> EmailAccountProvider.MAILGUN;
            case 5 -> EmailAccountProvider.AWS_SES;
            case 6 -> EmailAccountProvider.CUSTOM;
            case 7 -> EmailAccountProvider.RESEND;
            default -> null;
        };
    }

    /** One specification, used by the rows, the cards and the record walk. */
    private Specification<EmailAccount> buildSpec(EmailAccountFilter filter) {
        Specification<EmailAccount> spec = Specification.unrestricted();

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(EmailAccountSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getEmail() != null && !filter.getEmail().isBlank()) {
            spec = spec.and(EmailAccountSpecification.emailLike(filter.getEmail()));
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            spec = spec.and(EmailAccountSpecification.nameLike(filter.getName()));
        }
        if (filter.getDescription() != null && !filter.getDescription().isBlank()) {
            spec = spec.and(EmailAccountSpecification.descriptionLike(filter.getDescription()));
        }
        if (filter.getSmtpHost() != null && !filter.getSmtpHost().isBlank()) {
            spec = spec.and(EmailAccountSpecification.smtpHostLike(filter.getSmtpHost()));
        }
        if (filter.getSmtpUsername() != null && !filter.getSmtpUsername().isBlank()) {
            spec = spec.and(EmailAccountSpecification.smtpUsernameLike(filter.getSmtpUsername()));
        }
        if (filter.getErrorMessage() != null && !filter.getErrorMessage().isBlank()) {
            spec = spec.and(EmailAccountSpecification.errorMessageLike(filter.getErrorMessage()));
        }
        if (filter.getSmtpPort() != null) {
            spec = spec.and(EmailAccountSpecification.smtpPort(filter.getSmtpPort()));
        }
        if (!filter.allProviderTypes().isEmpty()) {
            spec = spec.and(EmailAccountSpecification.providerTypeIn(filter.allProviderTypes()));
        }
        Boolean enabled = filter.resolvedEnabled();
        if (enabled != null) spec = spec.and(EmailAccountSpecification.isEnabled(enabled));
        if (filter.getIsDefault() != null) spec = spec.and(EmailAccountSpecification.isDefault(filter.getIsDefault()));
        if (filter.getUseTls() != null) spec = spec.and(EmailAccountSpecification.useTls(filter.getUseTls()));
        if (filter.getUseSsl() != null) spec = spec.and(EmailAccountSpecification.useSsl(filter.getUseSsl()));

        if (Boolean.TRUE.equals(filter.getHasErrors()) || filter.wants("failing")) {
            spec = spec.and(EmailAccountSpecification.hasErrors());
        }
        if (filter.wants("neverTested")) spec = spec.and(EmailAccountSpecification.neverTested());
        if (filter.wants("receiving")) spec = spec.and(EmailAccountSpecification.isReceiving(true));
        if (filter.wants("fetchFailing")) spec = spec.and(EmailAccountSpecification.fetchFailing());
        if (filter.wants("neverFetched")) spec = spec.and(EmailAccountSpecification.neverFetched());
        if (filter.getCreatedAfter() != null) {
            spec = spec.and(EmailAccountSpecification.createdAfter(filter.getCreatedAfter()));
        }
        return spec;
    }

    /**
     * The cards over the accounts.
     *
     * Sending mail is the one thing this system does that a customer sees directly, and it
     * fails quietly: a broken account produces no error anybody here reads, only an invoice
     * that never arrives. So the figures are the silent failures — last send failed, never
     * tested, and on the receiving side, fetching that has stopped.
     */
    private Map<String, Object> buildStats(Specification<EmailAccount> spec) {
        return listStats.of(EmailAccount.class, spec)
            .total()
            .count("enabled", EmailAccountSpecification.isEnabled(true))
            .complement("disabled", "enabled")
            .count("failing", EmailAccountSpecification.hasErrors())
            .count("neverTested", EmailAccountSpecification.neverTested())
            .count("receiving", EmailAccountSpecification.isReceiving(true))
            .count("fetchFailing", EmailAccountSpecification.fetchFailing())
            .count("neverFetched", EmailAccountSpecification.neverFetched())
            .breakdown("byProvider", EmailAccountProvider.values(),
                type -> EmailAccountSpecification.providerTypeIn(List.of(type)))
            .recency(EmailAccountSpecification::createdAfter)
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
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
     * @param providerTypeLong Filter by provider type (as enum)
     * @param smtpHost Filter by SMTP host (partial match)
     * @param smtpPort Filter by SMTP port
     * @param hasErrors Filter by error status
     * @param description Filter by description (partial match)
     * @param useTls Filter by TLS enabled status
     * @param useSsl Filter by SSL enabled status
     * @param smtpUsername Filter by SMTP username (partial match)
     * @param errorMessage Filter by error message (partial match)
     * @param sortDirection Sort direction ("asc" or "desc")
     * @return ResponseEntity with paginated results or validation error
     */
    /** Kept so any caller still passing loose parameters keeps working. */
    public ResponseEntity<?> getAllEmailAccounts(
        int page,
        int size,
        Boolean enabled,
        Boolean isDefault,
        String email,
        String name,
        int providerTypeLong,
        String smtpHost,
        Integer smtpPort,
        Boolean hasErrors,
        String description,
        Boolean useTls,
        Boolean useSsl,
        String smtpUsername,
        String errorMessage,
        String sortBy,
        String sortDirection
    ) {
        EmailAccountFilter filter = new EmailAccountFilter();
        filter.setEnabled(enabled);
        filter.setIsDefault(isDefault);
        filter.setEmail(email);
        filter.setName(name);
        filter.setProviderType(providerFromInt(providerTypeLong));
        filter.setSmtpHost(smtpHost);
        filter.setSmtpPort(smtpPort);
        filter.setHasErrors(hasErrors);
        filter.setDescription(description);
        filter.setUseTls(useTls);
        filter.setUseSsl(useSsl);
        filter.setSmtpUsername(smtpUsername);
        filter.setErrorMessage(errorMessage);
        return getAllEmailAccounts(filter, null, page, size, sortBy, sortDirection);
    }

    public ResponseEntity<?> getAllEmailAccounts(
        EmailAccountFilter filter,
        Boolean includeStats,
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) {
        EmailAccountFilter active = filter != null ? filter : new EmailAccountFilter();

        // Validate sort field
        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            log.warn("Invalid sort field: {}", sortBy);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        // Validate pagination parameters
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            return ResponseEntity.badRequest().body("Page number cannot be negative");
        }
        if (size <= 0) {
            log.warn("Invalid page size: {}", size);
            return ResponseEntity.badRequest().body("Page size must be greater than 0");
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }
        // clamp: an unbounded size is a way to ask for the whole table by accident
        Pageable paging = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, validatedSortBy));

        Specification<EmailAccount> specification = buildSpec(active);

        Page<EmailAccount> pagedEmailAccounts = emailAccountRepository.findAll(specification, paging);

        // Convert to DTOs
        List<EmailAccountDTO> emailAccountDTOs = getEmailAccountDTOs(pagedEmailAccounts.getContent());

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("emailAccounts", emailAccountDTOs);
        response.put("currentPage", pagedEmailAccounts.getNumber());
        response.put("totalItems", pagedEmailAccounts.getTotalElements());
        response.put("totalPages", pagedEmailAccounts.getTotalPages());
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("pageSize", pagedEmailAccounts.getSize());
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        /*
         * Counters for the WHOLE filtered set, from the same specification as the rows, so a
         * card and the table under it cannot disagree.
         */
        if (!Boolean.FALSE.equals(includeStats)) {
            response.put("stats", buildStats(specification));
        }

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
        return getEmailAccount(idObfuscated, null, null, null);
    }

    /** One account, plus where it sits in the set the caller was looking at. */
    public ResponseEntity<ApiResponse<?>> getEmailAccount(
        String idObfuscated,
        EmailAccountFilter filter,
        String sortBy,
        String sortDirection
    ) {
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

            // Navigation IDs
            // the same specification and order the list used, so prev/next stays in that set
            Specification<EmailAccount> navSpec =
                buildSpec(filter != null ? filter : new EmailAccountFilter());
            String navSortBy = validateSortField(sortBy) != null ? validateSortField(sortBy) : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                EmailAccount.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("emailAccount", convertToDTO(emailAccount));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    "Successfully retrieved email account.",
                    response
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
                .apiKeyConfigured(emailAccount.getApiKey() != null && !emailAccount.getApiKey().isBlank())
                .webhookSecretConfigured(emailAccount.getWebhookSecret() != null && !emailAccount.getWebhookSecret().isBlank())
                .sendingMethod(emailAccount.getSendingMethod())
                .rateLimitPerMinute(emailAccount.getRateLimitPerMinute())
                .maxRetryAttempts(emailAccount.getMaxRetryAttempts())
                .retryDelaySeconds(emailAccount.getRetryDelaySeconds())
                .lastTestedAt(emailAccount.getLastTestedAt())
                .lastErrorMessage(emailAccount.getLastErrorMessage())
                .includeSignatureByDefault(emailAccount.getIncludeSignatureByDefault())
                .emailsSentCount(emailAccount.getEmailsSentCount())
                .emailsFailedCount(emailAccount.getEmailsFailedCount())
                .createdAt(emailAccount.getCreatedAt())
                .updatedAt(emailAccount.getUpdatedAt())
                .createdBy(emailAccount.getCreatedBy())
                .updatedBy(emailAccount.getUpdatedBy())
                .receivingProtocol(emailAccount.getReceivingProtocol())
                .imapHost(emailAccount.getImapHost())
                .imapPort(emailAccount.getImapPort())
                .imapUseSsl(emailAccount.getImapUseSsl())
                .imapUseTls(emailAccount.getImapUseTls())
                .imapUsername(emailAccount.getImapUsername())
                .imapPasswordConfigured(emailAccount.getImapPassword() != null
                    && !emailAccount.getImapPassword().isBlank())
                .receivingEnabled(emailAccount.getReceivingEnabled())
                .fetchIntervalMinutes(emailAccount.getFetchIntervalMinutes())
                .maxFetchCount(emailAccount.getMaxFetchCount())
                .lastFetchedAt(emailAccount.getLastFetchedAt())
                .lastFetchErrorMessage(emailAccount.getLastFetchErrorMessage())
                .consecutiveFetchFailures(emailAccount.getConsecutiveFetchFailures())
                .emailsReceivedCount(emailAccount.getEmailsReceivedCount())
                .build();
    }
}
