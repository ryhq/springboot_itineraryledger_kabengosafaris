package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationEmailRepository;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs.AccommodationEmailDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs.UpdateAccommodationEmailDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail;
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
 * UpdateAccommodationEmailService - Service for updating accommodation emails
 */
@Service
@Slf4j
@Transactional
public class UpdateAccommodationEmailService {

    private final AccommodationEmailRepository accommodationEmailRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateAccommodationEmailService(
        AccommodationEmailRepository accommodationEmailRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationEmailRepository = accommodationEmailRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an accommodation email by obfuscated ID
     *
     * @param idObfuscated The obfuscated email ID
     * @param updateDTO The updated email data
     * @return ResponseEntity with ApiResponse containing the updated email
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_EMAIL",
        description = "Updating accommodation email",
        entityType = "AccommodationEmail",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateAccommodationEmail(String idObfuscated, UpdateAccommodationEmailDTO updateDTO) {
        log.info("Updating accommodation email with ID: {}", idObfuscated);

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

            return updateAccommodationEmailById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating accommodation email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update accommodation email",
                    "ACCOMMODATION_EMAIL_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update an accommodation email by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateAccommodationEmailById(Long id, UpdateAccommodationEmailDTO updateDTO) {
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

        // Check if email is being changed and if it's unique
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(email.getEmail())) {
            if (accommodationEmailRepository.existsByEmail(updateDTO.getEmail())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Email already exists",
                        "DUPLICATE_EMAIL"
                    )
                );
            }
            email.setEmail(updateDTO.getEmail());
        }

        // Update other fields if provided
        if (updateDTO.getEmailType() != null) {
            email.setEmailType(updateDTO.getEmailType());
        }
        if (updateDTO.getIsPrimary() != null) {
            // If setting this email as primary, mark all other emails for this accommodation as non-primary
            if (updateDTO.getIsPrimary()) {
                Long accommodationId = email.getAccommodation().getId();
                accommodationEmailRepository.markAllAsNonPrimaryExcept(accommodationId, email.getId());
                log.info("Marked all other emails as non-primary for accommodation: {}", accommodationId);
            }
            email.setIsPrimary(updateDTO.getIsPrimary());
        }
        if (updateDTO.getIsActive() != null) {
            email.setIsActive(updateDTO.getIsActive());
        }
        if (updateDTO.getLabel() != null) {
            email.setLabel(updateDTO.getLabel());
        }

        // Save updated email
        email = accommodationEmailRepository.save(email);

        // Convert to DTO
        AccommodationEmailDTO emailDTO = convertToDTO(email);

        log.info("Accommodation email updated successfully: {}", email.getEmail());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Accommodation email updated successfully",
                emailDTO
            )
        );
    }

    /**
     * Convert AccommodationEmail entity to DTO
     */
    private AccommodationEmailDTO convertToDTO(AccommodationEmail email) {
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
