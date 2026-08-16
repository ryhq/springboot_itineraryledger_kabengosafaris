package com.itineraryledger.kabengosafaris.Translation.Account.Services;

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

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository;
import com.itineraryledger.kabengosafaris.Translation.Account.DTOs.TranslationAccountDTO;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationProviderType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TranslationAccountGetService {

    private final TranslationAccountRepository translationAccountRepository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "providerType", "enabled", "isDefault", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public TranslationAccountGetService(
        TranslationAccountRepository translationAccountRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.translationAccountRepository = translationAccountRepository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    /** One specification, used by the rows, the cards and the record walk. */
    private Specification<TranslationAccount> buildSpec(TranslationAccountFilter filter) {
        Specification<TranslationAccount> spec = Specification.unrestricted();

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            spec = spec.and(TranslationAccountSpecification.searchKeyword(filter.getKeyword()));
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            spec = spec.and(TranslationAccountSpecification.nameLike(filter.getName()));
        }
        if (filter.getDescription() != null && !filter.getDescription().isBlank()) {
            spec = spec.and(TranslationAccountSpecification.descriptionLike(filter.getDescription()));
        }
        if (filter.getBaseUrl() != null && !filter.getBaseUrl().isBlank()) {
            spec = spec.and(TranslationAccountSpecification.baseUrlLike(filter.getBaseUrl()));
        }
        if (!filter.allProviderTypes().isEmpty()) {
            spec = spec.and(TranslationAccountSpecification.providerTypeIn(filter.allProviderTypes()));
        }
        Boolean enabled = filter.resolvedEnabled();
        if (enabled != null) {
            spec = spec.and(TranslationAccountSpecification.isEnabled(enabled));
        }
        if (filter.getIsDefault() != null) {
            spec = spec.and(TranslationAccountSpecification.isDefault(filter.getIsDefault()));
        }
        if (Boolean.TRUE.equals(filter.getHasErrors()) || filter.wants("failing")) {
            spec = spec.and(TranslationAccountSpecification.hasErrors());
        }
        if (filter.wants("neverTested")) {
            spec = spec.and(TranslationAccountSpecification.neverTested());
        }
        if (filter.getCreatedAfter() != null) {
            spec = spec.and(TranslationAccountSpecification.createdAfter(filter.getCreatedAfter()));
        }
        return spec;
    }

    /**
     * The cards over the accounts.
     *
     * A translation provider fails quietly — the text just comes back in English — so the
     * two figures that matter are the ones nobody would otherwise notice: which accounts
     * last failed, and which have never been proved to work at all.
     */
    private Map<String, Object> buildStats(Specification<TranslationAccount> spec) {
        return listStats.of(TranslationAccount.class, spec)
            .total()
            .count("enabled", TranslationAccountSpecification.isEnabled(true))
            .complement("disabled", "enabled")
            .count("failing", TranslationAccountSpecification.hasErrors())
            .count("neverTested", TranslationAccountSpecification.neverTested())
            .count("isDefault", TranslationAccountSpecification.isDefault(true))
            .breakdown("byProvider", TranslationProviderType.values(),
                type -> TranslationAccountSpecification.providerTypeIn(List.of(type)))
            .recency(TranslationAccountSpecification::createdAfter)
            .build();
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /** Kept so any caller still passing loose parameters keeps working. */
    public ResponseEntity<?> getAllTranslationAccounts(
        int page,
        int size,
        Boolean enabled,
        Boolean isDefault,
        String name,
        int providerTypeInt,
        Boolean hasErrors,
        String description,
        String baseUrl,
        String sortBy,
        String sortDirection
    ) {
        TranslationAccountFilter filter = new TranslationAccountFilter();
        filter.setEnabled(enabled);
        filter.setIsDefault(isDefault);
        filter.setName(name);
        if (providerTypeInt > 0) filter.setProviderType(providerTypeInt);
        filter.setHasErrors(hasErrors);
        filter.setDescription(description);
        filter.setBaseUrl(baseUrl);
        return getAllTranslationAccounts(filter, null, page, size, sortBy, sortDirection);
    }

    public ResponseEntity<?> getAllTranslationAccounts(
        TranslationAccountFilter filter,
        Boolean includeStats,
        int page,
        int size,
        String sortBy,
        String sortDirection
    ) {
        TranslationAccountFilter active = filter != null ? filter : new TranslationAccountFilter();
        // Validate sort field
        String validatedSortBy = validateSortField(sortBy);
        if (validatedSortBy == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
            );
        }

        if (page < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Page number cannot be negative", "INVALID_PAGE"));
        }
        if (size <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Page size must be greater than 0", "INVALID_SIZE"));
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }
        // clamp: an unbounded size is a way to ask for the whole table by accident
        Pageable paging = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, validatedSortBy));

        Specification<TranslationAccount> specification = buildSpec(active);

        Page<TranslationAccount> pagedAccounts = translationAccountRepository.findAll(specification, paging);

        List<TranslationAccountDTO> dtos = pagedAccounts.getContent().stream()
                .map(this::convertToDTO).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("translationAccounts", dtos);
        response.put("currentPage", pagedAccounts.getNumber());
        response.put("totalItems", pagedAccounts.getTotalElements());
        response.put("totalPages", pagedAccounts.getTotalPages());
        response.put("validSortFields", VALID_SORT_FIELDS);
        response.put("currentSortBy", validatedSortBy);
        response.put("pageSize", pagedAccounts.getSize());
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
        /*
         * Counters for the WHOLE filtered set, from the same specification as the rows, so a
         * card and the table under it cannot disagree.
         */
        if (!Boolean.FALSE.equals(includeStats)) {
            response.put("stats", buildStats(specification));
        }

        return ResponseEntity.ok(
            ApiResponse.success(200, "Successfully retrieved translation accounts.", response)
        );
    }

    public ResponseEntity<ApiResponse<?>> getTranslationAccount(String idObfuscated) {
        return getTranslationAccount(idObfuscated, null, null, null);
    }

    /** One account, plus where it sits in the set the caller was looking at. */
    public ResponseEntity<ApiResponse<?>> getTranslationAccount(
        String idObfuscated,
        TranslationAccountFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);

            TranslationAccount account = translationAccountRepository.findById(id).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Translation account not found", "TRANSLATION_ACCOUNT_NOT_FOUND")
                );
            }

            // the same specification and order the list used, so prev/next stays in that set
            Specification<TranslationAccount> navSpec =
                buildSpec(filter != null ? filter : new TranslationAccountFilter());
            String navSortBy = validateSortField(sortBy) != null ? validateSortField(sortBy) : DEFAULT_SORT_FIELD;
            boolean ascending = "asc".equalsIgnoreCase(sortDirection);

            Map<String, Object> nav = recordNavigation.navigate(
                TranslationAccount.class, navSpec, navSortBy, ascending, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("translationAccount", convertToDTO(account));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(
                ApiResponse.success(200, "Successfully retrieved translation account.", response)
            );

        } catch (Exception e) {
            log.error("Error getting translation account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get translation account", "GET_TRANSLATION_ACCOUNT_FAILED")
            );
        }
    }

    public TranslationAccountDTO convertToDTO(TranslationAccount account) {
        return TranslationAccountDTO.builder()
                .id(idObfuscator.encodeId(account.getId()))
                .name(account.getName())
                .description(account.getDescription())
                .providerType(account.getProviderType())
                .baseUrl(account.getBaseUrl())
                .enabled(account.getEnabled())
                .isDefault(account.getIsDefault())
                .timeoutSeconds(account.getTimeoutSeconds())
                .lastTestedAt(account.getLastTestedAt())
                .lastErrorMessage(account.getLastErrorMessage())
                .charactersTranslated(account.getCharactersTranslated())
                .requestsMade(account.getRequestsMade())
                .requestsFailed(account.getRequestsFailed())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .createdBy(account.getCreatedBy())
                .updatedBy(account.getUpdatedBy())
                .build();
    }
}
