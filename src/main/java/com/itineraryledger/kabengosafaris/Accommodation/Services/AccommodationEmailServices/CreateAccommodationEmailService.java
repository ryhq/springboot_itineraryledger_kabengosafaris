package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationEmailRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs.AccommodationEmailDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationEmailDTOs.CreateAccommodationEmailDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
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
 * CreateAccommodationEmailService - Service for creating accommodation emails
 */
@Service
@Slf4j
public class CreateAccommodationEmailService {

    private final AccommodationEmailRepository accommodationEmailRepository;
    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateAccommodationEmailService(
        AccommodationEmailRepository accommodationEmailRepository,
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationEmailRepository = accommodationEmailRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation email
     *
     * @param createDTO The email data
     * @return ResponseEntity with ApiResponse containing the created email
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_EMAIL",
        description = "Creating a new accommodation email",
        entityType = "AccommodationEmail"
    )
    public ResponseEntity<ApiResponse<?>> createAccommodationEmail(CreateAccommodationEmailDTO createDTO) {
        log.info("Creating new accommodation email: {}", createDTO.getEmail());

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Decode accommodation ID
            Long accommodationId;
            try {
                accommodationId = idObfuscator.decodeId(createDTO.getAccommodationId());
            } catch (Exception e) {
                log.warn("Invalid accommodation ID: {}", createDTO.getAccommodationId());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Find accommodation
            Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null) {
                log.warn("Accommodation not found: {}", accommodationId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Accommodation not found",
                        "ACCOMMODATION_NOT_FOUND"
                    )
                );
            }

            // Check for duplicate email
            /* On THIS accommodation. Two sister properties share one reservations desk. */
            if (accommodationEmailRepository.existsByAccommodationIdAndEmail(
                    accommodation.getId(), createDTO.getEmail())) {
                log.warn("Email already exists: {}", createDTO.getEmail());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Email already exists",
                        "DUPLICATE_EMAIL"
                    )
                );
            }

            // If this email is being set as primary, mark all other emails for this accommodation as non-primary
            boolean isPrimary = createDTO.getIsPrimary() != null && createDTO.getIsPrimary();
            if (isPrimary) {
                accommodationEmailRepository.markAllAsNonPrimaryForAccommodation(accommodationId);
                log.info("Marked all existing emails as non-primary for accommodation: {}", accommodationId);
            }

            // Create email entity
            AccommodationEmail email = AccommodationEmail.builder()
                .accommodation(accommodation)
                .email(createDTO.getEmail())
                .emailType(createDTO.getEmailType())
                .isPrimary(isPrimary)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .label(createDTO.getLabel())
                .build();

            // Save to database
            AccommodationEmail savedEmail = accommodationEmailRepository.save(email);

            log.info("Accommodation email created successfully with ID: {}", savedEmail.getId());

            // Create response DTO
            AccommodationEmailDTO emailDTO = convertToDTO(savedEmail);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Accommodation email created successfully",
                    emailDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating accommodation email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create accommodation email: " + e.getMessage(),
                    "ACCOMMODATION_EMAIL_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for email creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreateAccommodationEmailDTO dto) {
        // Validate and sanitize email
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Email address cannot be empty", "INVALID_EMAIL")
            );
        }

        String trimmedEmail = dto.getEmail().trim().toLowerCase();

        // Validate email length (max 254 characters as per RFC 5321)
        if (trimmedEmail.length() > 254) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Email address cannot exceed 254 characters", "EMAIL_TOO_LONG")
            );
        }

        // Validate email format using RFC 5322 compliant regex
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";
        if (!trimmedEmail.matches(emailRegex)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid email address format", "INVALID_EMAIL_FORMAT")
            );
        }

        // Validate local part length (before @, max 64 characters as per RFC 5321)
        String[] parts = trimmedEmail.split("@");
        if (parts.length == 2 && parts[0].length() > 64) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Email local part cannot exceed 64 characters", "EMAIL_LOCAL_PART_TOO_LONG")
            );
        }

        // Validate domain part length (after @, max 253 characters)
        if (parts.length == 2 && parts[1].length() > 253) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Email domain cannot exceed 253 characters", "EMAIL_DOMAIN_TOO_LONG")
            );
        }

        dto.setEmail(trimmedEmail);

        // Validate label length if provided
        if (dto.getLabel() != null && dto.getLabel().length() > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Label cannot exceed 100 characters", "LABEL_TOO_LONG")
            );
        }

        return null; // No validation errors
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
