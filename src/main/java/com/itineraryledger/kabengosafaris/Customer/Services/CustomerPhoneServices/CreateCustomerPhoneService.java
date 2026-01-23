package com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerPhoneRepository;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs.CustomerPhoneDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs.CreateCustomerPhoneDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
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
 * CreateCustomerPhoneService - Service for creating customer phones
 */
@Service
@Slf4j
public class CreateCustomerPhoneService {

    private final CustomerPhoneRepository customerPhoneRepository;
    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateCustomerPhoneService(
        CustomerPhoneRepository customerPhoneRepository,
        CustomerRepository customerRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerPhoneRepository = customerPhoneRepository;
        this.customerRepository = customerRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new customer phone
     *
     * @param createDTO The phone data
     * @return ResponseEntity with ApiResponse containing the created phone
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_CUSTOMER_PHONE",
        description = "Creating a new customer phone",
        entityType = "CustomerPhone"
    )
    public ResponseEntity<ApiResponse<?>> createCustomerPhone(CreateCustomerPhoneDTO createDTO) {
        log.info("Creating new customer phone: {}", createDTO.getPhoneNumber());

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

            // Check for duplicate phone number
            if (customerPhoneRepository.existsByPhoneNumber(createDTO.getPhoneNumber())) {
                log.warn("Phone number already exists: {}", createDTO.getPhoneNumber());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Phone number already exists",
                        "DUPLICATE_PHONE_NUMBER"
                    )
                );
            }

            // If this phone is being set as primary, mark all other phones for this customer as non-primary
            boolean isPrimary = createDTO.getIsPrimary() != null && createDTO.getIsPrimary();
            if (isPrimary) {
                customerPhoneRepository.markAllAsNonPrimaryForCustomer(customerId);
                log.info("Marked all existing phones as non-primary for customer: {}", customerId);
            }

            // Create phone entity
            CustomerPhone phone = CustomerPhone.builder()
                .customer(customer)
                .phoneNumber(createDTO.getPhoneNumber())
                .countryCode(createDTO.getCountryCode())
                .phoneType(createDTO.getPhoneType())
                .isPrimary(isPrimary)
                .isWhatsApp(createDTO.getIsWhatsApp() != null ? createDTO.getIsWhatsApp() : false)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .label(createDTO.getLabel())
                .build();

            // Save to database
            CustomerPhone savedPhone = customerPhoneRepository.save(phone);

            log.info("Customer phone created successfully with ID: {}", savedPhone.getId());

            // Create response DTO
            CustomerPhoneDTO phoneDTO = convertToDTO(savedPhone);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Customer phone created successfully",
                    phoneDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating customer phone", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create customer phone: " + e.getMessage(),
                    "CUSTOMER_PHONE_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Validate input fields for phone creation
     */
    private ResponseEntity<ApiResponse<?>> validateInputFields(CreateCustomerPhoneDTO dto) {
        // Validate and sanitize phone number
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Phone number cannot be empty", "INVALID_PHONE_NUMBER")
            );
        }

        String trimmedPhone = dto.getPhoneNumber().trim();

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
