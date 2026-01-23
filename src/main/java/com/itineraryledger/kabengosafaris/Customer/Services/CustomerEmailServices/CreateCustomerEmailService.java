package com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.CustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.CreateCustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
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
 * CreateCustomerEmailService - Service for creating customer emails
 */
@Service
@Slf4j
public class CreateCustomerEmailService {

    private final CustomerEmailRepository customerEmailRepository;
    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateCustomerEmailService(
        CustomerEmailRepository customerEmailRepository,
        CustomerRepository customerRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerEmailRepository = customerEmailRepository;
        this.customerRepository = customerRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new customer email
     *
     * @param createDTO The email data
     * @return ResponseEntity with ApiResponse containing the created email
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_CUSTOMER_EMAIL",
        description = "Creating a new customer email",
        entityType = "CustomerEmail"
    )
    public ResponseEntity<ApiResponse<?>> createCustomerEmail(CreateCustomerEmailDTO createDTO) {
        log.info("Creating new customer email: {}", createDTO.getEmail());

        try {
            // Validate input fields
            ResponseEntity<ApiResponse<?>> validationError = validateInputFields(createDTO);
            if (validationError != null) {
                return validationError;
            }

            // Decode customer ID
            Long customerId;
            try {
                customerId = idObfuscator.decodeId(createDTO.getCustomerId());
            } catch (Exception e) {
                log.warn("Invalid customer ID: {}", createDTO.getCustomerId());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid customer ID",
                        "INVALID_CUSTOMER_ID"
                    )
                );
            }

            // Find customer
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                log.warn("Customer not found: {}", customerId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Customer not found",
                        "CUSTOMER_NOT_FOUND"
                    )
                );
            }

            // Check for duplicate email
            if (customerEmailRepository.existsByEmail(createDTO.getEmail())) {
                log.warn("Email already exists: {}", createDTO.getEmail());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Email already exists",
                        "DUPLICATE_EMAIL"
                    )
                );
            }

            // If this email is being set as primary, mark all other emails for this customer as non-primary
            boolean isPrimary = createDTO.getIsPrimary() != null && createDTO.getIsPrimary();
            if (isPrimary) {
                customerEmailRepository.markAllAsNonPrimaryForCustomer(customerId);
                log.info("Marked all existing emails as non-primary for customer: {}", customerId);
            }

            // Create email entity
            CustomerEmail email = CustomerEmail.builder()
                .customer(customer)
                .email(createDTO.getEmail())
                .emailType(createDTO.getEmailType())
                .isPrimary(isPrimary)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .label(createDTO.getLabel())
                .build();

            // Save to database
            CustomerEmail savedEmail = customerEmailRepository.save(email);

            log.info("Customer email created successfully with ID: {}", savedEmail.getId());

            // Create response DTO
            CustomerEmailDTO emailDTO = convertToDTO(savedEmail);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Customer email created successfully",
                    emailDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating customer email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create customer email: " + e.getMessage(),
                    "CUSTOMER_EMAIL_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for email creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreateCustomerEmailDTO dto) {
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
