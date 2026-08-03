package com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.CustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomerEmailGetService - Service for retrieving customer emails
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomerEmailGetService {

    private final CustomerEmailRepository customerEmailRepository;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "email", "emailType", "label", "isPrimary", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public CustomerEmailGetService(
        CustomerEmailRepository customerEmailRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats,
        com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation
    ) {
        this.customerEmailRepository = customerEmailRepository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
        this.recordNavigation = recordNavigation;
    }

    /**
     * Get customer email by obfuscated ID
     *
     * @param idObfuscated The obfuscated email ID
     * @return ResponseEntity with ApiResponse containing the email
     */
    public ResponseEntity<ApiResponse<?>> getCustomerEmailById(String idObfuscated, String scopeParentId) {
        log.info("Fetching customer email with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode email ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid email ID",
                        "INVALID_EMAIL_ID"
                    )
                );
            }

            // Find email
            CustomerEmail email = customerEmailRepository.findById(id).orElse(null);
            if (email == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Customer email not found",
                        "CUSTOMER_EMAIL_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            CustomerEmailDTO emailDTO = convertToDTO(email);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            // Circular navigation (scoped if parent provided, global otherwise)
            Long nextId, previousId;
            if (decodedParentId != null) {
                nextId = customerEmailRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = customerEmailRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = customerEmailRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = customerEmailRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = customerEmailRepository.findNextId(id).orElse(null);
                previousId = customerEmailRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = customerEmailRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = customerEmailRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("email", emailDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            // the "3 of 6" readout: without it, wrapping past the last record is invisible
            response.putAll(recordNavigation.positionOf(
                CustomerEmail.class, "customer.id", decodedParentId, id));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer email retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer email",
                    "CUSTOMER_EMAIL_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all customer emails with filtering and pagination
     * customerId is an optional filter parameter
     *
     * @param customerId Optional obfuscated customer ID filter
     * @param email Filter by email address (optional)
     * @param emailType Filter by email type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated emails
     */
    public ResponseEntity<ApiResponse<?>> getAllCustomerEmails(
        String customerId,
        String email,
        EmailType emailType,
        Boolean isPrimary,
        Boolean isActive,
        String label,
        String keyword,
        java.util.List<CustomerEmail.EmailType> emailTypes,
        java.util.List<String> statuses,
        java.util.List<String> qualities,
        java.time.LocalDateTime createdAfter,
        java.time.LocalDateTime createdBefore,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all customer emails with optional filters");

        try {
            // Build specification
            Specification<CustomerEmail> spec = Specification.unrestricted();

            // Add optional customer ID filter
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedCustomerId = idObfuscator.decodeId(customerId);
                    spec = spec.and(CustomerEmailSpecification.hasCustomerId(decodedCustomerId));
                } catch (Exception e) {
                    log.warn("Failed to decode customer ID: {}", customerId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid customer ID",
                            "INVALID_CUSTOMER_ID"
                        )
                    );
                }
            }

            // Add other optional filters
            if (email != null && !email.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.emailLike(email));
            }
            if (emailType != null) {
                spec = spec.and(CustomerEmailSpecification.hasEmailType(emailType));
            }
            if (isPrimary != null) {
                spec = spec.and(CustomerEmailSpecification.isPrimary(isPrimary));
            }
            if (isActive != null) {
                spec = spec.and(CustomerEmailSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch paginated results
            // multi-value facets, so every stat card is also a filter
            if (emailTypes != null && !emailTypes.isEmpty()) spec = spec.and(CustomerEmailSpecification.emailTypeIn(emailTypes));
            if (statuses != null && !statuses.isEmpty()) {
                java.util.List<Boolean> states = new java.util.ArrayList<>();
                if (statuses.contains("active")) states.add(true);
                if (statuses.contains("inactive")) states.add(false);
                if (states.size() == 1) spec = spec.and(CustomerEmailSpecification.isActive(states.get(0)));
            }
            if (qualities != null && qualities.contains("no-label")) spec = spec.and(CustomerEmailSpecification.missingLabel());
            if (createdAfter != null) spec = spec.and(CustomerEmailSpecification.createdAfter(createdAfter));
            if (createdBefore != null) spec = spec.and(CustomerEmailSpecification.createdBefore(createdBefore));
            Page<CustomerEmail> emailPage = customerEmailRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerEmailDTO> emailDTOPage = emailPage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("emails", emailDTOPage.getContent());
            responseData.put("currentPage", emailDTOPage.getNumber());
            responseData.put("totalItems", emailDTOPage.getTotalElements());
            responseData.put("totalPages", emailDTOPage.getTotalPages());
            responseData.put("pageSize", emailDTOPage.getSize());
            responseData.put("validSortFields", VALID_SORT_FIELDS);
            responseData.put("currentSortBy", validatedSortBy);
            responseData.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            if (!Boolean.FALSE.equals(includeStats)) {
                responseData.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer emails retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer emails",
                    "CUSTOMER_EMAILS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all emails for a specific customer
     * customerId is REQUIRED
     *
     * @param customerId Required obfuscated customer ID
     * @param email Filter by email address (optional)
     * @param emailType Filter by email type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated emails for the customer
     */
    public ResponseEntity<ApiResponse<?>> getCustomersEmails(
        @NotBlank(message = "Customer ID is required") String customerId,
        String email,
        EmailType emailType,
        Boolean isPrimary,
        Boolean isActive,
        String label,
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching emails for customer: {}", customerId);

        try {
            // Decode customer ID (required)
            Long decodedCustomerId;
            try {
                decodedCustomerId = idObfuscator.decodeId(customerId);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", customerId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid customer ID",
                        "INVALID_CUSTOMER_ID"
                    )
                );
            }

            // Build specification with required customer ID filter
            Specification<CustomerEmail> spec = CustomerEmailSpecification.hasCustomerId(decodedCustomerId);

            // Add optional filters
            if (email != null && !email.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.emailLike(email));
            }
            if (emailType != null) {
                spec = spec.and(CustomerEmailSpecification.hasEmailType(emailType));
            }
            if (isPrimary != null) {
                spec = spec.and(CustomerEmailSpecification.isPrimary(isPrimary));
            }
            if (isActive != null) {
                spec = spec.and(CustomerEmailSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch paginated results
            Page<CustomerEmail> emailPage = customerEmailRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerEmailDTO> emailDTOPage = emailPage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("emails", emailDTOPage.getContent());
            responseData.put("currentPage", emailDTOPage.getNumber());
            responseData.put("totalItems", emailDTOPage.getTotalElements());
            responseData.put("totalPages", emailDTOPage.getTotalPages());
            responseData.put("pageSize", emailDTOPage.getSize());
            responseData.put("validSortFields", VALID_SORT_FIELDS);
            responseData.put("currentSortBy", validatedSortBy);
            responseData.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer emails retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer emails",
                    "CUSTOMER_EMAILS_FETCH_FAILED"
                )
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert CustomerEmail entity to DTO
     */
    public CustomerEmailDTO convertToDTO(CustomerEmail email) {
        return CustomerEmailDTO.builder()
            .id(idObfuscator.encodeId(email.getId()))
            .customerId(idObfuscator.encodeId(email.getCustomer().getId()))
            .customerDisplayName(email.getCustomer().getDisplayName())
            .email(email.getEmail())
            .emailType(email.getEmailType())
            .emailTypeDisplayName(email.getEmailType() != null ? email.getEmailType().getDisplayName() : null)
            .emailTypeDescription(email.getEmailType() != null ? email.getEmailType().getDescription() : null)
            .isPrimary(email.getIsPrimary())
            .isActive(email.getIsActive())
            .label(email.getLabel())
            .createdAt(email.getCreatedAt())
            .updatedAt(email.getUpdatedAt())
            .build();
    }

    /** Dashboard counters for the CURRENT filter set (stats on every list). */
    private Map<String, Object> computeStats(Specification<CustomerEmail> base) {
        return listStats.of(CustomerEmail.class, base)
            .total()
            .count("active", CustomerEmailSpecification.isActive(true)).complement("inactive", "active")
            .count("primary", CustomerEmailSpecification.isPrimary(true))
            .count("missingLabel", CustomerEmailSpecification.missingLabel())
            .breakdown("byEmailType", CustomerEmail.EmailType.values(), CustomerEmailSpecification::hasEmailType)
            .recency(CustomerEmailSpecification::createdAfter)
            .build();
    }
}
