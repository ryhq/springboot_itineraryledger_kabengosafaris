package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationPhoneRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs.AccommodationPhoneDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationPhoneDTOs.CreateAccommodationPhoneDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
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
 * CreateAccommodationPhoneService - Service for creating accommodation phones
 */
@Service
@Slf4j
@Transactional
public class CreateAccommodationPhoneService {

    private final AccommodationPhoneRepository accommodationPhoneRepository;
    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateAccommodationPhoneService(
        AccommodationPhoneRepository accommodationPhoneRepository,
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationPhoneRepository = accommodationPhoneRepository;
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation phone
     *
     * @param createDTO The phone data
     * @return ResponseEntity with ApiResponse containing the created phone
     */
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION_PHONE",
        description = "Creating accommodation phone",
        entityType = "AccommodationPhone"
    )
    public ResponseEntity<ApiResponse<?>> createAccommodationPhone(CreateAccommodationPhoneDTO createDTO) {
        log.info("Creating new accommodation phone: {}", createDTO.getPhoneNumber());

        try {
            // Decode accommodation ID
            Long accommodationId;
            try {
                accommodationId = idObfuscator.decodeId(createDTO.getAccommodationId());
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", createDTO.getAccommodationId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Check if accommodation exists
            Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Accommodation not found",
                        "ACCOMMODATION_NOT_FOUND"
                    )
                );
            }

            // Check for duplicate phone number
            if (accommodationPhoneRepository.existsByPhoneNumber(createDTO.getPhoneNumber())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Phone number already exists",
                        "DUPLICATE_PHONE_NUMBER"
                    )
                );
            }

            // If this phone is being set as primary, mark all other phones for this accommodation as non-primary
            boolean isPrimary = createDTO.getIsPrimary() != null && createDTO.getIsPrimary();
            if (isPrimary) {
                accommodationPhoneRepository.markAllAsNonPrimaryForAccommodation(accommodationId);
                log.info("Marked all existing phones as non-primary for accommodation: {}", accommodationId);
            }

            // Create phone entity
            AccommodationPhone phone = AccommodationPhone.builder()
                .accommodation(accommodation)
                .phoneNumber(createDTO.getPhoneNumber())
                .countryCode(createDTO.getCountryCode())
                .phoneType(createDTO.getPhoneType())
                .isPrimary(isPrimary)
                .isWhatsApp(createDTO.getIsWhatsApp() != null ? createDTO.getIsWhatsApp() : false)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .label(createDTO.getLabel())
                .operatingHours(createDTO.getOperatingHours())
                .build();

            // Save phone
            phone = accommodationPhoneRepository.save(phone);

            // Convert to DTO
            AccommodationPhoneDTO phoneDTO = convertToDTO(phone);

            log.info("Accommodation phone created successfully: {}", phone.getPhoneNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Accommodation phone created successfully",
                    phoneDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating accommodation phone", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create accommodation phone",
                    "ACCOMMODATION_PHONE_CREATE_FAILED"
                )
            );
        }
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
