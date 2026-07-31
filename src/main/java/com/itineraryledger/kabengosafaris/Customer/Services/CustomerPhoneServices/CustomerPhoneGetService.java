package com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerPhoneRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs.CustomerPhoneDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType;
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
 * CustomerPhoneGetService - Service for retrieving customer phones
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomerPhoneGetService {

    private final CustomerPhoneRepository customerPhoneRepository;
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "phoneNumber", "phoneType", "label", "isPrimary", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public CustomerPhoneGetService(
        CustomerPhoneRepository customerPhoneRepository,
        IdObfuscator idObfuscator,
        com.itineraryledger.kabengosafaris.Response.ListStats listStats
    ) {
        this.customerPhoneRepository = customerPhoneRepository;
        this.idObfuscator = idObfuscator;
        this.listStats = listStats;
    }

    /**
     * Get customer phone by obfuscated ID
     *
     * @param idObfuscated The obfuscated phone ID
     * @return ResponseEntity with ApiResponse containing the phone
     */
    public ResponseEntity<ApiResponse<?>> getCustomerPhoneById(String idObfuscated, String scopeParentId) {
        log.info("Fetching customer phone with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode phone ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid phone ID",
                        "INVALID_PHONE_ID"
                    )
                );
            }

            // Find phone
            CustomerPhone phone = customerPhoneRepository.findById(id).orElse(null);
            if (phone == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Customer phone not found",
                        "CUSTOMER_PHONE_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            CustomerPhoneDTO phoneDTO = convertToDTO(phone);

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
                nextId = customerPhoneRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = customerPhoneRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = customerPhoneRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = customerPhoneRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = customerPhoneRepository.findNextId(id).orElse(null);
                previousId = customerPhoneRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = customerPhoneRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = customerPhoneRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("phone", phoneDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer phone retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer phone", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer phone",
                    "CUSTOMER_PHONE_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all customer phones with filtering and pagination
     * customerId is an optional filter parameter
     *
     * @param customerId Optional obfuscated customer ID filter
     * @param phoneNumber Filter by phone number (optional)
     * @param phoneType Filter by phone type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isWhatsApp Filter by WhatsApp status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated phones
     */
    public ResponseEntity<ApiResponse<?>> getAllCustomerPhones(
        String customerId,
        String phoneNumber,
        PhoneType phoneType,
        Boolean isPrimary,
        Boolean isWhatsApp,
        Boolean isActive,
        String label,
        String keyword,
        java.util.List<CustomerPhone.PhoneType> phoneTypes,
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
        log.info("Fetching all customer phones with optional filters");

        try {
            // Build specification
            Specification<CustomerPhone> spec = Specification.unrestricted();

            // Add optional customer ID filter
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedCustomerId = idObfuscator.decodeId(customerId);
                    spec = spec.and(CustomerPhoneSpecification.hasCustomerId(decodedCustomerId));
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
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                spec = spec.and(CustomerPhoneSpecification.phoneNumberLike(phoneNumber));
            }
            if (phoneType != null) {
                spec = spec.and(CustomerPhoneSpecification.hasPhoneType(phoneType));
            }
            if (isPrimary != null) {
                spec = spec.and(CustomerPhoneSpecification.isPrimary(isPrimary));
            }
            if (isWhatsApp != null) {
                spec = spec.and(CustomerPhoneSpecification.isWhatsApp(isWhatsApp));
            }
            if (isActive != null) {
                spec = spec.and(CustomerPhoneSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(CustomerPhoneSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerPhoneSpecification.searchKeyword(keyword));
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
            if (phoneTypes != null && !phoneTypes.isEmpty()) spec = spec.and(CustomerPhoneSpecification.phoneTypeIn(phoneTypes));
            if (statuses != null && !statuses.isEmpty()) {
                java.util.List<Boolean> states = new java.util.ArrayList<>();
                if (statuses.contains("active")) states.add(true);
                if (statuses.contains("inactive")) states.add(false);
                if (states.size() == 1) spec = spec.and(CustomerPhoneSpecification.isActive(states.get(0)));
            }
            if (qualities != null && qualities.contains("no-label")) spec = spec.and(CustomerPhoneSpecification.missingLabel());
            if (qualities != null && qualities.contains("no-country-code")) spec = spec.and(CustomerPhoneSpecification.missingCountryCode());
            if (createdAfter != null) spec = spec.and(CustomerPhoneSpecification.createdAfter(createdAfter));
            if (createdBefore != null) spec = spec.and(CustomerPhoneSpecification.createdBefore(createdBefore));
            Page<CustomerPhone> phonePage = customerPhoneRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerPhoneDTO> phoneDTOPage = phonePage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("phones", phoneDTOPage.getContent());
            responseData.put("currentPage", phoneDTOPage.getNumber());
            responseData.put("totalItems", phoneDTOPage.getTotalElements());
            responseData.put("totalPages", phoneDTOPage.getTotalPages());
            responseData.put("pageSize", phoneDTOPage.getSize());
            responseData.put("validSortFields", VALID_SORT_FIELDS);
            responseData.put("currentSortBy", validatedSortBy);
            responseData.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");
            if (!Boolean.FALSE.equals(includeStats)) {
                responseData.put("stats", computeStats(spec));
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer phones retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer phones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer phones",
                    "CUSTOMER_PHONES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all phones for a specific customer
     * customerId is REQUIRED
     *
     * @param customerId Required obfuscated customer ID
     * @param phoneNumber Filter by phone number (optional)
     * @param phoneType Filter by phone type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isWhatsApp Filter by WhatsApp status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated phones for the customer
     */
    public ResponseEntity<ApiResponse<?>> getCustomersPhones(
        @NotBlank(message = "Customer ID is required") String customerId,
        String phoneNumber,
        PhoneType phoneType,
        Boolean isPrimary,
        Boolean isWhatsApp,
        Boolean isActive,
        String label,
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching phones for customer: {}", customerId);

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
            Specification<CustomerPhone> spec = CustomerPhoneSpecification.hasCustomerId(decodedCustomerId);

            // Add optional filters
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                spec = spec.and(CustomerPhoneSpecification.phoneNumberLike(phoneNumber));
            }
            if (phoneType != null) {
                spec = spec.and(CustomerPhoneSpecification.hasPhoneType(phoneType));
            }
            if (isPrimary != null) {
                spec = spec.and(CustomerPhoneSpecification.isPrimary(isPrimary));
            }
            if (isWhatsApp != null) {
                spec = spec.and(CustomerPhoneSpecification.isWhatsApp(isWhatsApp));
            }
            if (isActive != null) {
                spec = spec.and(CustomerPhoneSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(CustomerPhoneSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerPhoneSpecification.searchKeyword(keyword));
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
            Page<CustomerPhone> phonePage = customerPhoneRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerPhoneDTO> phoneDTOPage = phonePage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("phones", phoneDTOPage.getContent());
            responseData.put("currentPage", phoneDTOPage.getNumber());
            responseData.put("totalItems", phoneDTOPage.getTotalElements());
            responseData.put("totalPages", phoneDTOPage.getTotalPages());
            responseData.put("pageSize", phoneDTOPage.getSize());
            responseData.put("validSortFields", VALID_SORT_FIELDS);
            responseData.put("currentSortBy", validatedSortBy);
            responseData.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer phones retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer phones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer phones",
                    "CUSTOMER_PHONES_FETCH_FAILED"
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
     * Convert CustomerPhone entity to DTO
     */
    public CustomerPhoneDTO convertToDTO(CustomerPhone phone) {
        return CustomerPhoneDTO.builder()
            .id(idObfuscator.encodeId(phone.getId()))
            .customerId(idObfuscator.encodeId(phone.getCustomer().getId()))
            .customerDisplayName(phone.getCustomer().getDisplayName())
            .phoneNumber(phone.getPhoneNumber())
            .countryCode(phone.getCountryCode())
            .phoneType(phone.getPhoneType())
            .phoneTypeDisplayName(phone.getPhoneType() != null ? phone.getPhoneType().getDisplayName() : null)
            .phoneTypeDescription(phone.getPhoneType() != null ? phone.getPhoneType().getDescription() : null)
            .isPrimary(phone.getIsPrimary())
            .isWhatsApp(phone.getIsWhatsApp())
            .isActive(phone.getIsActive())
            .label(phone.getLabel())
            .createdAt(phone.getCreatedAt())
            .updatedAt(phone.getUpdatedAt())
            .build();
    }

    /** Dashboard counters for the CURRENT filter set (stats on every list). */
    private Map<String, Object> computeStats(Specification<CustomerPhone> base) {
        return listStats.of(CustomerPhone.class, base)
            .total()
            .count("active", CustomerPhoneSpecification.isActive(true)).complement("inactive", "active")
            .count("primary", CustomerPhoneSpecification.isPrimary(true))
            .count("whatsApp", CustomerPhoneSpecification.isWhatsApp(true))
            .count("missingLabel", CustomerPhoneSpecification.missingLabel())
            .count("missingCountryCode", CustomerPhoneSpecification.missingCountryCode())
            .breakdown("byPhoneType", CustomerPhone.PhoneType.values(), CustomerPhoneSpecification::hasPhoneType)
            .recency(CustomerPhoneSpecification::createdAfter)
            .build();
    }
}
