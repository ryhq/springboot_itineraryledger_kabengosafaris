package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationPhoneRepository;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs.AccommodationPhoneDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone.PhoneType;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * AccommodationPhoneGetService - Service for retrieving accommodation phones
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationPhoneGetService {

    private final AccommodationPhoneRepository accommodationPhoneRepository;

    // filter-aware prev/next + the N of M readout

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    // dashboard counters for the CURRENT filter set (see CLAUDE.md)

    @org.springframework.beans.factory.annotation.Autowired

    private com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "phoneNumber", "phoneType", "label", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public AccommodationPhoneGetService(
        AccommodationPhoneRepository accommodationPhoneRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationPhoneRepository = accommodationPhoneRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get accommodation phone by obfuscated ID
     *
     * @param idObfuscated The obfuscated phone ID
     * @return ResponseEntity with ApiResponse containing the phone
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationPhoneById(String idObfuscated, String scopeParentId) {
        return getAccommodationPhoneById(idObfuscated, scopeParentId, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * One record, plus where it sits in the set the caller was looking at.
     *
     * The list's filters and sort arrive here because paging out of a filtered
     * list must stay inside that filter, and the N of M readout must count the
     * same set. Arrows that traverse a different set are worse than no arrows.
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationPhoneById(
        String idObfuscated,
        String scopeParentId,
        /*
         * The global list filters by accommodation through a facet, not a scope; both
         * forms have to reach the walk or paging escapes the filter on screen.
         */
        String accommodationId,
        String phoneNumber,
        String countryCode,
        PhoneType phoneType,
        Boolean isPrimary,
        Boolean isWhatsApp,
        Boolean isActive,
        String label,
        String keyword,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching accommodation phone with ID: {}", idObfuscated);

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
            AccommodationPhone phone = accommodationPhoneRepository.findById(id).orElse(null);
            if (phone == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Accommodation phone not found",
                        "ACCOMMODATION_PHONE_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            AccommodationPhoneDTO phoneDTO = convertToDTO(phone);

            // Decode optional scope parent ID for scoped navigation
            Long decodedParentId = null;
            if (scopeParentId != null && !scopeParentId.isEmpty()) {
                try {
                    decodedParentId = idObfuscator.decodeId(scopeParentId);
                } catch (Exception ex) {
                    log.warn("Invalid scopeParentId: {}, falling back to global navigation", scopeParentId);
                }
            }

            /*
             * Prev/next walks the SAME set the caller was looking at — this parent's
             * children when scoped, everything otherwise — and returns the position so
             * the record page can show 'N of M' with the wraparound visible.
             */
            Specification<AccommodationPhone> navSpec = buildSpec(decodedParentId != null ? decodedParentId : decodeOrNull(accommodationId), phoneNumber, countryCode, phoneType, isPrimary, isWhatsApp, isActive, label, keyword);
            String navSortBy = validateSortField(sortBy);
            if (navSortBy == null) navSortBy = DEFAULT_SORT_FIELD;
            boolean navAscending = sortDirection != null && sortDirection.equalsIgnoreCase("asc");
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                AccommodationPhone.class, navSpec, navSortBy, navAscending, id
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("phone", phoneDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation phone retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation phone", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation phone",
                    "ACCOMMODATION_PHONE_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all accommodation phones with filtering and pagination
     * accommodationId is an optional filter parameter
     *
     * @param accommodationId Optional obfuscated accommodation ID filter
     * @param phoneNumber Filter by phone number (partial match)
     * @param countryCode Filter by country code
     * @param phoneType Filter by phone type
     * @param isPrimary Filter by primary status
     * @param isWhatsApp Filter by WhatsApp status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated phones
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationPhones(
        String accommodationId,
        String phoneNumber,
        String countryCode,
        PhoneType phoneType,
        Boolean isPrimary,
        Boolean isWhatsApp,
        Boolean isActive,
        String label,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching all accommodation phones with optional filters");

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Build specification
            Long decodedAccommodationId = null;
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    decodedAccommodationId = idObfuscator.decodeId(accommodationId);
                } catch (Exception e) {
                    log.warn("Failed to decode accommodation ID: {}", accommodationId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid accommodation ID", "INVALID_ACCOMMODATION_ID")
                    );
                }
            }
            Specification<AccommodationPhone> spec = buildSpec(decodedAccommodationId, phoneNumber, countryCode, phoneType, isPrimary, isWhatsApp, isActive, label, keyword);

            // Fetch paginated results
            Page<AccommodationPhone> phonePage = accommodationPhoneRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<AccommodationPhoneDTO> phoneDTOPage = phonePage.map(this::convertToDTO);

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
            // counters share the rows' Specification, so cards and table agree
            responseData.put("stats", computeStats(spec));

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation phones retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation phones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation phones",
                    "ACCOMMODATION_PHONES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all phones for a specific accommodation
     * accommodationId is REQUIRED
     *
     * @param accommodationId Required obfuscated accommodation ID
     * @param phoneNumber Filter by phone number (partial match)
     * @param countryCode Filter by country code
     * @param phoneType Filter by phone type
     * @param isPrimary Filter by primary status
     * @param isWhatsApp Filter by WhatsApp status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated phones for the accommodation
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsPhones(
        @NotBlank(message = "Accommodation ID is required") String accommodationId,
        String phoneNumber,
        String countryCode,
        PhoneType phoneType,
        Boolean isPrimary,
        Boolean isWhatsApp,
        Boolean isActive,
        String label,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching phones for accommodation: {}", accommodationId);

        try {
            // Validate sort field
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            // Decode accommodation ID (required)
            Long decodedAccommodationId;
            try {
                decodedAccommodationId = idObfuscator.decodeId(accommodationId);
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", accommodationId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Build specification with required accommodation ID filter
            Specification<AccommodationPhone> spec = AccommodationPhoneSpecification.hasAccommodationId(decodedAccommodationId);

            // Add optional filters
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                spec = spec.and(AccommodationPhoneSpecification.phoneNumberLike(phoneNumber));
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                spec = spec.and(AccommodationPhoneSpecification.hasCountryCode(countryCode));
            }
            if (phoneType != null) {
                spec = spec.and(AccommodationPhoneSpecification.hasPhoneType(phoneType));
            }
            if (isPrimary != null) {
                spec = spec.and(AccommodationPhoneSpecification.isPrimary(isPrimary));
            }
            if (isWhatsApp != null) {
                spec = spec.and(AccommodationPhoneSpecification.isWhatsApp(isWhatsApp));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationPhoneSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(AccommodationPhoneSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationPhoneSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<AccommodationPhone> phonePage = accommodationPhoneRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<AccommodationPhoneDTO> phoneDTOPage = phonePage.map(this::convertToDTO);

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
                    "Accommodation phones retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation phones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation phones",
                    "ACCOMMODATION_PHONES_FETCH_FAILED"
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
     * Convert AccommodationPhone entity to DTO
     */
    public AccommodationPhoneDTO convertToDTO(AccommodationPhone phone) {
        return AccommodationPhoneDTO.builder()
            .id(idObfuscator.encodeId(phone.getId()))
            .accommodationId(idObfuscator.encodeId(phone.getAccommodation().getId()))
            .accommodationName(phone.getAccommodation().getName())
            .phoneNumber(phone.getPhoneNumber())
            .countryCode(phone.getCountryCode())
            .phoneType(phone.getPhoneType())
            .phoneTypeDisplayName(phone.getPhoneType() != null ? phone.getPhoneType().getDisplayName() : null)
            .phoneTypeDescription(phone.getPhoneType() != null ? phone.getPhoneType().getDescription() : null)
            .isPrimary(phone.getIsPrimary())
            .isWhatsApp(phone.getIsWhatsApp())
            .isActive(phone.getIsActive())
            .label(phone.getLabel())
            .operatingHours(phone.getOperatingHours())
            .createdAt(phone.getCreatedAt())
            .updatedAt(phone.getUpdatedAt())
            .build();
    }

    /** Dashboard counters built from the SAME Specification as the rows. */
    private java.util.Map<String, Object> computeStats(
        org.springframework.data.jpa.domain.Specification<AccommodationPhone> base
    ) {
        return listStats.of(AccommodationPhone.class, base)
            .total()
            .count("active", AccommodationPhoneSpecification.isActive(true))
            .complement("inactive", "active")
            .recency(AccommodationPhoneSpecification::createdAfter)
            .build();
    }

    /**
     * The ONE place a AccommodationPhone filter is expressed.
     *
     * The rows, the stat counters and prev/next paging all build from this, so a
     * card can never disagree with the table and the arrows can never walk a
     * different set from the one on screen.
     */
    private Specification<AccommodationPhone> buildSpec(
        Long accommodationId,
        String phoneNumber,
        String countryCode,
        PhoneType phoneType,
        Boolean isPrimary,
        Boolean isWhatsApp,
        Boolean isActive,
        String label,
        String keyword
    ) {
        Specification<AccommodationPhone> spec = Specification.unrestricted();
        if (accommodationId != null) {
            spec = spec.and(AccommodationPhoneSpecification.hasAccommodationId(accommodationId));
        }
    if (phoneNumber != null && !phoneNumber.isEmpty()) {
    spec = spec.and(AccommodationPhoneSpecification.phoneNumberLike(phoneNumber));
    }
    if (countryCode != null && !countryCode.isEmpty()) {
    spec = spec.and(AccommodationPhoneSpecification.hasCountryCode(countryCode));
    }
    if (phoneType != null) {
    spec = spec.and(AccommodationPhoneSpecification.hasPhoneType(phoneType));
    }
    if (isPrimary != null) {
    spec = spec.and(AccommodationPhoneSpecification.isPrimary(isPrimary));
    }
    if (isWhatsApp != null) {
    spec = spec.and(AccommodationPhoneSpecification.isWhatsApp(isWhatsApp));
    }
    if (isActive != null) {
    spec = spec.and(AccommodationPhoneSpecification.isActive(isActive));
    }
    if (label != null && !label.isEmpty()) {
    spec = spec.and(AccommodationPhoneSpecification.labelLike(label));
    }
    if (keyword != null && !keyword.isEmpty()) {
    spec = spec.and(AccommodationPhoneSpecification.searchKeyword(keyword));
    }
        return spec;
    }

    /** Decodes an obfuscated id, or null when absent or unreadable. */
    private Long decodeOrNull(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            log.warn("Unreadable id in filter: {}", obfuscated);
            return null;
        }
    }
}
