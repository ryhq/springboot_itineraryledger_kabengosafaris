package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationPhoneRepository;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs.AccommodationPhoneDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs.UpdateAccommodationPhoneDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UpdateAccommodationPhoneService - Service for updating accommodation phones
 */
@Service
@Slf4j
@Transactional
public class UpdateAccommodationPhoneService {

    private final AccommodationPhoneRepository accommodationPhoneRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateAccommodationPhoneService(
        AccommodationPhoneRepository accommodationPhoneRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationPhoneRepository = accommodationPhoneRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an accommodation phone by obfuscated ID
     *
     * @param idObfuscated The obfuscated phone ID
     * @param updateDTO The updated phone data
     * @return ResponseEntity with ApiResponse containing the updated phone
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_PHONE",
        description = "Updating accommodation phone",
        entityType = "AccommodationPhone",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateAccommodationPhone(String idObfuscated, UpdateAccommodationPhoneDTO updateDTO) {
        log.info("Updating accommodation phone with ID: {}", idObfuscated);

        try {
            // Validate that at least one field is provided for update
            ResponseEntity<ApiResponse<?>> validationError = validateAtLeastOneFieldProvided(updateDTO);
            if (validationError != null) {
                return validationError;
            }

            // Validate input fields
            validationError = validateInputFields(updateDTO);
            if (validationError != null) {
                return validationError;
            }

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

            return updateAccommodationPhoneById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating accommodation phone", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update accommodation phone",
                    "ACCOMMODATION_PHONE_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update an accommodation phone by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateAccommodationPhoneById(Long id, UpdateAccommodationPhoneDTO updateDTO) {
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

        // Check if phone number is being changed and if it's unique
        if (updateDTO.getPhoneNumber() != null && !updateDTO.getPhoneNumber().equals(phone.getPhoneNumber())) {
            if (accommodationPhoneRepository.existsByAccommodationIdAndPhoneNumberAndIdNot(
                    phone.getAccommodation().getId(), updateDTO.getPhoneNumber(), phone.getId())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Phone number already exists",
                        "DUPLICATE_PHONE_NUMBER"
                    )
                );
            }
            phone.setPhoneNumber(updateDTO.getPhoneNumber());
        }

        // Update other fields if provided
        if (updateDTO.getCountryCode() != null) {
            phone.setCountryCode(updateDTO.getCountryCode());
        }
        if (updateDTO.getPhoneType() != null) {
            phone.setPhoneType(updateDTO.getPhoneType().isBlank() ? null : com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone.PhoneType.valueOf(updateDTO.getPhoneType().trim()));
        }
        if (updateDTO.getIsPrimary() != null) {
            // If setting this phone as primary, mark all other phones for this accommodation as non-primary
            if (updateDTO.getIsPrimary()) {
                Long accommodationId = phone.getAccommodation().getId();
                accommodationPhoneRepository.markAllAsNonPrimaryExcept(accommodationId, phone.getId());
                log.info("Marked all other phones as non-primary for accommodation: {}", accommodationId);
            }
            phone.setIsPrimary(updateDTO.getIsPrimary());
        }
        if (updateDTO.getIsWhatsApp() != null) {
            phone.setIsWhatsApp(updateDTO.getIsWhatsApp());
        }
        if (updateDTO.getIsActive() != null) {
            phone.setIsActive(updateDTO.getIsActive());
        }
        if (updateDTO.getLabel() != null) {
            phone.setLabel(updateDTO.getLabel());
        }
        if (updateDTO.getOperatingHours() != null) {
            phone.setOperatingHours(updateDTO.getOperatingHours());
        }

        // Save updated phone
        phone = accommodationPhoneRepository.save(phone);

        // Convert to DTO
        AccommodationPhoneDTO phoneDTO = convertToDTO(phone);

        log.info("Accommodation phone updated successfully: {}", phone.getPhoneNumber());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Accommodation phone updated successfully",
                phoneDTO
            )
        );
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateAccommodationPhoneDTO dto) {
        boolean hasUpdate =
            dto.getPhoneNumber() != null ||
            dto.getCountryCode() != null ||
            dto.getPhoneType() != null ||
            dto.getIsPrimary() != null ||
            dto.getIsWhatsApp() != null ||
            dto.getIsActive() != null ||
            dto.getLabel() != null ||
            dto.getOperatingHours() != null;

        if (!hasUpdate) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "At least one field must be provided for update", "NO_FIELDS_TO_UPDATE")
            );
        }

        return null;
    }

    /**
     * Validate input fields for phone update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateAccommodationPhoneDTO dto) {
        // Validate and sanitize phone number if provided
        if (dto.getPhoneNumber() != null) {
            String trimmedPhoneNumber = dto.getPhoneNumber().trim();

            if (trimmedPhoneNumber.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Phone number cannot be empty", "INVALID_PHONE_NUMBER")
                );
            }

            // Validate phone number length (minimum 7 digits, maximum 15 digits as per E.164 standard)
            if (trimmedPhoneNumber.length() < 7) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Phone number must be at least 7 characters", "PHONE_NUMBER_TOO_SHORT")
                );
            }

            if (trimmedPhoneNumber.length() > 15) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Phone number cannot exceed 15 characters", "PHONE_NUMBER_TOO_LONG")
                );
            }

            // Validate phone number format (should contain only digits, spaces, +, -, (, ))
            if (!trimmedPhoneNumber.matches("^[0-9+\\-\\s()]+$")) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Phone number contains invalid characters", "INVALID_PHONE_NUMBER_FORMAT")
                );
            }

            dto.setPhoneNumber(trimmedPhoneNumber);
        }

        // Validate country code if provided
        if (dto.getCountryCode() != null) {
            String trimmedCountryCode = dto.getCountryCode().trim();

            if (trimmedCountryCode.length() > 5) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Country code cannot exceed 5 characters", "COUNTRY_CODE_TOO_LONG")
                );
            }

            // Validate country code format (should start with + and contain only digits)
            if (!trimmedCountryCode.matches("^\\+?[0-9]+$")) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Country code must contain only digits and optionally start with +", "INVALID_COUNTRY_CODE_FORMAT")
                );
            }

            dto.setCountryCode(trimmedCountryCode);
        }

        // Validate label length if provided
        if (dto.getLabel() != null && dto.getLabel().length() > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Label cannot exceed 100 characters", "LABEL_TOO_LONG")
            );
        }

        // Validate operating hours length if provided
        if (dto.getOperatingHours() != null && dto.getOperatingHours().length() > 255) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Operating hours cannot exceed 255 characters", "OPERATING_HOURS_TOO_LONG")
            );
        }

        return null; // No validation errors
    }

    /**
     * Convert AccommodationPhone entity to DTO
     */
    private AccommodationPhoneDTO convertToDTO(AccommodationPhone phone) {
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
