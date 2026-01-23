package com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.CustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.UpdateCustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
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
 * UpdateCustomerEmailService - Service for updating customer emails
 */
@Service
@Slf4j
@Transactional
public class UpdateCustomerEmailService {

    private final CustomerEmailRepository customerEmailRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateCustomerEmailService(
        CustomerEmailRepository customerEmailRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerEmailRepository = customerEmailRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a customer email by obfuscated ID
     *
     * @param idObfuscated The obfuscated email ID
     * @param updateDTO The updated email data
     * @return ResponseEntity with ApiResponse containing the updated email
     */
    @AuditLogAnnotation(
        action = "UPDATE_CUSTOMER_EMAIL",
        description = "Updating customer email",
        entityType = "CustomerEmail",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateCustomerEmail(String idObfuscated, UpdateCustomerEmailDTO updateDTO) {
        log.info("Updating customer email with ID: {}", idObfuscated);

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
                log.warn("Failed to decode email ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid email ID",
                        "INVALID_EMAIL_ID"
                    )
                );
            }

            return updateCustomerEmailById(id, updateDTO);

        } catch (Exception e) {
            log.error("Error updating customer email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update customer email",
                    "CUSTOMER_EMAIL_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update a customer email by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateCustomerEmailById(Long id, UpdateCustomerEmailDTO updateDTO) {
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

        // Check if email is being changed and if it's unique
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(email.getEmail())) {
            if (customerEmailRepository.existsByEmail(updateDTO.getEmail())) {
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
            // If setting this email as primary, mark all other emails for this customer as non-primary
            if (updateDTO.getIsPrimary()) {
                Long customerId = email.getCustomer().getId();
                customerEmailRepository.markAllAsNonPrimaryExcept(customerId, email.getId());
                log.info("Marked all other emails as non-primary for customer: {}", customerId);
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
        email = customerEmailRepository.save(email);

        // Convert to DTO
        CustomerEmailDTO emailDTO = convertToDTO(email);

        log.info("Customer email updated successfully: {}", email.getEmail());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Customer email updated successfully",
                emailDTO
            )
        );
    }

    /**
     * Validate that at least one field is provided for update
     */
    private ResponseEntity<ApiResponse<?>> validateAtLeastOneFieldProvided(UpdateCustomerEmailDTO dto) {
        boolean hasUpdate =
            dto.getEmail() != null ||
            dto.getEmailType() != null ||
            dto.getIsPrimary() != null ||
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
     * Validate input fields for email update
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(UpdateCustomerEmailDTO dto) {
        // Validate and sanitize email if provided
        if (dto.getEmail() != null) {
            String trimmedEmail = dto.getEmail().trim();

            if (trimmedEmail.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Email address cannot be empty", "INVALID_EMAIL")
                );
            }

            trimmedEmail = trimmedEmail.toLowerCase();

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
     * Convert CustomerEmail entity to DTO
     */
    private CustomerEmailDTO convertToDTO(CustomerEmail email) {
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
}
