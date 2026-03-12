package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationEmailRepository;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs.AccommodationEmailDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail.EmailType;
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
 * AccommodationEmailGetService - Service for retrieving accommodation emails
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationEmailGetService {

    private final AccommodationEmailRepository accommodationEmailRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "email", "emailType", "label", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public AccommodationEmailGetService(
        AccommodationEmailRepository accommodationEmailRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationEmailRepository = accommodationEmailRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get accommodation email by obfuscated ID
     *
     * @param idObfuscated The obfuscated email ID
     * @return ResponseEntity with ApiResponse containing the email
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationEmailById(String idObfuscated, String scopeParentId) {
        log.info("Fetching accommodation email with ID: {}", idObfuscated);

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
            AccommodationEmail email = accommodationEmailRepository.findById(id).orElse(null);
            if (email == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Accommodation email not found",
                        "ACCOMMODATION_EMAIL_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            AccommodationEmailDTO emailDTO = convertToDTO(email);

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
                nextId = accommodationEmailRepository.findNextIdByParent(id, decodedParentId).orElse(null);
                previousId = accommodationEmailRepository.findPreviousIdByParent(id, decodedParentId).orElse(null);
                if (nextId == null) nextId = accommodationEmailRepository.findFirstIdByParent(decodedParentId).orElse(null);
                if (previousId == null) previousId = accommodationEmailRepository.findLastIdByParent(decodedParentId).orElse(null);
            } else {
                nextId = accommodationEmailRepository.findNextId(id).orElse(null);
                previousId = accommodationEmailRepository.findPreviousId(id).orElse(null);
                if (nextId == null) nextId = accommodationEmailRepository.findFirstId().orElse(null);
                if (previousId == null) previousId = accommodationEmailRepository.findLastId().orElse(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("email", emailDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation email retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation email",
                    "ACCOMMODATION_EMAIL_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all accommodation emails with filtering and pagination
     * accommodationId is an optional filter parameter
     *
     * @param accommodationId Optional obfuscated accommodation ID filter
     * @param email Filter by email address (optional)
     * @param emailType Filter by email type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated emails
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationEmails(
        String accommodationId,
        String email,
        EmailType emailType,
        Boolean isPrimary,
        Boolean isActive,
        String label,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching all accommodation emails with optional filters");

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
            Specification<AccommodationEmail> spec = Specification.unrestricted();

            // Add optional accommodation ID filter
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    Long decodedAccommodationId = idObfuscator.decodeId(accommodationId);
                    spec = spec.and(AccommodationEmailSpecification.hasAccommodationId(decodedAccommodationId));
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
            }

            // Add other optional filters
            if (email != null && !email.isEmpty()) {
                spec = spec.and(AccommodationEmailSpecification.emailLike(email));
            }
            if (emailType != null) {
                spec = spec.and(AccommodationEmailSpecification.hasEmailType(emailType));
            }
            if (isPrimary != null) {
                spec = spec.and(AccommodationEmailSpecification.isPrimary(isPrimary));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationEmailSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(AccommodationEmailSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationEmailSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<AccommodationEmail> emailPage = accommodationEmailRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<AccommodationEmailDTO> emailDTOPage = emailPage.map(this::convertToDTO);

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
                    "Accommodation emails retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation emails",
                    "ACCOMMODATION_EMAILS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all emails for a specific accommodation
     * accommodationId is REQUIRED
     *
     * @param accommodationId Required obfuscated accommodation ID
     * @param email Filter by email address (optional)
     * @param emailType Filter by email type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated emails for the accommodation
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodationsEmails(
        @NotBlank(message = "Accommodation ID is required") String accommodationId,
        String email,
        EmailType emailType,
        Boolean isPrimary,
        Boolean isActive,
        String label,
        String keyword,
        String sortBy,
        String sortDirection,
        Pageable pageable
    ) {
        log.info("Fetching emails for accommodation: {}", accommodationId);

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
            Specification<AccommodationEmail> spec = AccommodationEmailSpecification.hasAccommodationId(decodedAccommodationId);

            // Add optional filters
            if (email != null && !email.isEmpty()) {
                spec = spec.and(AccommodationEmailSpecification.emailLike(email));
            }
            if (emailType != null) {
                spec = spec.and(AccommodationEmailSpecification.hasEmailType(emailType));
            }
            if (isPrimary != null) {
                spec = spec.and(AccommodationEmailSpecification.isPrimary(isPrimary));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationEmailSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(AccommodationEmailSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationEmailSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<AccommodationEmail> emailPage = accommodationEmailRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<AccommodationEmailDTO> emailDTOPage = emailPage.map(this::convertToDTO);

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
                    "Accommodation emails retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation emails",
                    "ACCOMMODATION_EMAILS_FETCH_FAILED"
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
     * Convert AccommodationEmail entity to DTO
     */
    public AccommodationEmailDTO convertToDTO(AccommodationEmail email) {
        return AccommodationEmailDTO.builder()
            .id(idObfuscator.encodeId(email.getId()))
            .accommodationId(idObfuscator.encodeId(email.getAccommodation().getId()))
            .accommodationName(email.getAccommodation().getName())
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
}
