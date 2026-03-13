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

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "providerType", "enabled", "isDefault", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public TranslationAccountGetService(TranslationAccountRepository translationAccountRepository, IdObfuscator idObfuscator) {
        this.translationAccountRepository = translationAccountRepository;
        this.idObfuscator = idObfuscator;
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

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

        // Parse provider type
        TranslationProviderType providerType = null;
        if (providerTypeInt > 0) {
            try {
                providerType = TranslationProviderType.fromInteger(providerTypeInt);
            } catch (IllegalArgumentException e) {
                // Invalid provider type filter, ignore
            }
        }

        // Setup sorting
        Sort.Direction direction = Sort.Direction.DESC;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.ASC;
        }

        Pageable paging = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

        // Build specification
        Specification<TranslationAccount> specification = Specification.unrestricted();

        if (enabled != null) {
            specification = specification.and(TranslationAccountSpecification.isEnabled(enabled));
        }
        if (isDefault != null) {
            specification = specification.and(TranslationAccountSpecification.isDefault(isDefault));
        }
        if (name != null && !name.isBlank()) {
            specification = specification.and(TranslationAccountSpecification.nameLike(name));
        }
        if (providerType != null) {
            specification = specification.and(TranslationAccountSpecification.providerType(providerType));
        }
        if (hasErrors != null && hasErrors) {
            specification = specification.and(TranslationAccountSpecification.hasErrors());
        }
        if (description != null && !description.isBlank()) {
            specification = specification.and(TranslationAccountSpecification.descriptionLike(description));
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            specification = specification.and(TranslationAccountSpecification.baseUrlLike(baseUrl));
        }

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
        response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

        return ResponseEntity.ok(
            ApiResponse.success(200, "Successfully retrieved translation accounts.", response)
        );
    }

    public ResponseEntity<ApiResponse<?>> getTranslationAccount(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);

            TranslationAccount account = translationAccountRepository.findById(id).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Translation account not found", "TRANSLATION_ACCOUNT_NOT_FOUND")
                );
            }

            // Navigation IDs
            Long nextId = translationAccountRepository.findNextId(id).orElse(null);
            Long previousId = translationAccountRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = translationAccountRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = translationAccountRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("translationAccount", convertToDTO(account));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

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
