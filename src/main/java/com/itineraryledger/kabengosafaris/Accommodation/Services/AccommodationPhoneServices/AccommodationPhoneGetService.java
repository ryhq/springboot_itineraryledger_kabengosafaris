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

import java.util.HashMap;
import java.util.Map;

/**
 * AccommodationPhoneGetService - Service for retrieving accommodation phones
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationPhoneGetService {

    private final AccommodationPhoneRepository accommodationPhoneRepository;
    private final IdObfuscator idObfuscator;

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
    public ResponseEntity<ApiResponse<?>> getAccommodationPhoneById(String idObfuscated) {
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

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation phone retrieved successfully",
                    phoneDTO
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
        Pageable pageable
    ) {
        log.info("Fetching all accommodation phones with optional filters");

        try {
            // Build specification
            Specification<AccommodationPhone> spec = Specification.unrestricted();

            // Add optional accommodation ID filter
            if (accommodationId != null && !accommodationId.isEmpty()) {
                try {
                    Long decodedAccommodationId = idObfuscator.decodeId(accommodationId);
                    spec = spec.and(AccommodationPhoneSpecification.hasAccommodationId(decodedAccommodationId));
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
        Pageable pageable
    ) {
        log.info("Fetching phones for accommodation: {}", accommodationId);

        try {
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
}
