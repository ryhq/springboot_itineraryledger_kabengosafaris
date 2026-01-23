package com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerPhoneRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs.CustomerPhoneDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs.UpdateCustomerPhoneDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
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
 * UpdateCustomerPhoneService - Service for updating customer phones
 */
@Service
@Slf4j
@Transactional
public class UpdateCustomerPhoneService {

    private final CustomerPhoneRepository customerPhoneRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateCustomerPhoneService(
        CustomerPhoneRepository customerPhoneRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerPhoneRepository = customerPhoneRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a customer phone by obfuscated ID
     *
     * @param idObfuscated The obfuscated phone ID
     * @param updateDTO The updated phone data
     * @return ResponseEntity with ApiResponse containing the updated phone
     */
    @AuditLogAnnotation(
        action = "UPDATE_CUSTOMER_PHONE",
        description = "Updating customer phone",
        entityType = "CustomerPhone",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateCustomerPhone(String idObfuscated, UpdateCustomerPhoneDTO updateDTO) {
        log.info("Updating customer phone with ID: {}", idObfuscated);

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

            return updateCustomerPhoneById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating customer phone", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update customer phone",
                    "CUSTOMER_PHONE_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update a customer phone by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateCustomerPhoneById(Long id, UpdateCustomerPhoneDTO updateDTO) {
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

        // Check if phone number is being changed and if it's unique
        if (updateDTO.getPhoneNumber() != null && !updateDTO.getPhoneNumber().equals(phone.getPhoneNumber())) {
            if (customerPhoneRepository.existsByPhoneNumberAndIdNot(updateDTO.getPhoneNumber(), id)) {
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
            phone.setPhoneType(updateDTO.getPhoneType());
        }
        if (updateDTO.getIsPrimary() != null) {
            // If setting this phone as primary, mark all other phones for this customer as non-primary
            if (updateDTO.getIsPrimary()) {
                Long customerId = phone.getCustomer().getId();
                customerPhoneRepository.markAllAsNonPrimaryExcept(customerId, phone.getId());
                log.info("Marked all other phones as non-primary for customer: {}", customerId);
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

        // Save updated phone
        phone = customerPhoneRepository.save(phone);

        // Convert to DTO
        CustomerPhoneDTO phoneDTO = convertToDTO(phone);

        log.info("Customer phone updated successfully: {}", phone.getPhoneNumber());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Customer phone updated successfully",
                phoneDTO
            )
        );
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateCustomerPhoneDTO dto) {
        boolean hasUpdate =
            dto.getPhoneNumber() != null ||
            dto.getCountryCode() != null ||
            dto.getPhoneType() != null ||
            dto.getIsPrimary() != null ||
            dto.getIsWhatsApp() != null ||
            dto.getIsActive() != null ||
            dto.getLabel() != null;

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
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateCustomerPhoneDTO dto) {
        // Validate and sanitize phone number if provided
        if (dto.getPhoneNumber() != null) {
            String trimmedPhone = dto.getPhoneNumber().trim();

            if (trimmedPhone.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Phone number cannot be empty", "INVALID_PHONE_NUMBER")
                );
            }

            // Validate phone number length
            if (trimmedPhone.length() > 50) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Phone number cannot exceed 50 characters", "PHONE_NUMBER_TOO_LONG")
                );
            }

            // Validate phone number format (basic validation - allows digits, spaces, dashes, plus sign, parentheses)
            String phoneRegex = "^[+]?[0-9\\s\\-()]+$";
            if (!trimmedPhone.matches(phoneRegex)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid phone number format", "INVALID_PHONE_FORMAT")
                );
            }

            dto.setPhoneNumber(trimmedPhone);
        }

        // Validate country code if provided
        if (dto.getCountryCode() != null) {
            String trimmedCountryCode = dto.getCountryCode().trim();
            if (trimmedCountryCode.length() > 10) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Country code cannot exceed 10 characters", "COUNTRY_CODE_TOO_LONG")
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

        return null; // No validation errors
    }

    /**
     * Convert CustomerPhone entity to DTO
     */
    private CustomerPhoneDTO convertToDTO(CustomerPhone phone) {
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
}
