package com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.CustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
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
 * CustomerEmailGetService - Service for retrieving customer emails
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomerEmailGetService {

    private final CustomerEmailRepository customerEmailRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CustomerEmailGetService(
        CustomerEmailRepository customerEmailRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerEmailRepository = customerEmailRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get customer email by obfuscated ID
     *
     * @param idObfuscated The obfuscated email ID
     * @return ResponseEntity with ApiResponse containing the email
     */
    public ResponseEntity<ApiResponse<?>> getCustomerEmailById(String idObfuscated) {
        log.info("Fetching customer email with ID: {}", idObfuscated);

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

            // Convert to DTO
            CustomerEmailDTO emailDTO = convertToDTO(email);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer email retrieved successfully",
                    emailDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer email",
                    "CUSTOMER_EMAIL_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all customer emails with filtering and pagination
     * customerId is an optional filter parameter
     *
     * @param customerId Optional obfuscated customer ID filter
     * @param email Filter by email address (optional)
     * @param emailType Filter by email type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated emails
     */
    public ResponseEntity<ApiResponse<?>> getAllCustomerEmails(
        String customerId,
        String email,
        EmailType emailType,
        Boolean isPrimary,
        Boolean isActive,
        String label,
        String keyword,
        Pageable pageable
    ) {
        log.info("Fetching all customer emails with optional filters");

        try {
            // Build specification
            Specification<CustomerEmail> spec = Specification.unrestricted();

            // Add optional customer ID filter
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedCustomerId = idObfuscator.decodeId(customerId);
                    spec = spec.and(CustomerEmailSpecification.hasCustomerId(decodedCustomerId));
                } catch (Exception e) {
                    log.warn("Failed to decode customer ID: {}", customerId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid customer ID",
                            "INVALID_CUSTOMER_ID"
                        )
                    );
                }
            }

            // Add other optional filters
            if (email != null && !email.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.emailLike(email));
            }
            if (emailType != null) {
                spec = spec.and(CustomerEmailSpecification.hasEmailType(emailType));
            }
            if (isPrimary != null) {
                spec = spec.and(CustomerEmailSpecification.isPrimary(isPrimary));
            }
            if (isActive != null) {
                spec = spec.and(CustomerEmailSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<CustomerEmail> emailPage = customerEmailRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerEmailDTO> emailDTOPage = emailPage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("emails", emailDTOPage.getContent());
            responseData.put("currentPage", emailDTOPage.getNumber());
            responseData.put("totalItems", emailDTOPage.getTotalElements());
            responseData.put("totalPages", emailDTOPage.getTotalPages());
            responseData.put("pageSize", emailDTOPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer emails retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer emails",
                    "CUSTOMER_EMAILS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all emails for a specific customer
     * customerId is REQUIRED
     *
     * @param customerId Required obfuscated customer ID
     * @param email Filter by email address (optional)
     * @param emailType Filter by email type (optional)
     * @param isPrimary Filter by primary status (optional)
     * @param isActive Filter by active status (optional)
     * @param label Filter by label (optional)
     * @param keyword Search keyword across multiple fields (optional)
     * @param pageable Pagination and sorting parameters
     * @return ResponseEntity with ApiResponse containing paginated emails for the customer
     */
    public ResponseEntity<ApiResponse<?>> getCustomersEmails(
        @NotBlank(message = "Customer ID is required") String customerId,
        String email,
        EmailType emailType,
        Boolean isPrimary,
        Boolean isActive,
        String label,
        String keyword,
        Pageable pageable
    ) {
        log.info("Fetching emails for customer: {}", customerId);

        try {
            // Decode customer ID (required)
            Long decodedCustomerId;
            try {
                decodedCustomerId = idObfuscator.decodeId(customerId);
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", customerId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid customer ID",
                        "INVALID_CUSTOMER_ID"
                    )
                );
            }

            // Build specification with required customer ID filter
            Specification<CustomerEmail> spec = CustomerEmailSpecification.hasCustomerId(decodedCustomerId);

            // Add optional filters
            if (email != null && !email.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.emailLike(email));
            }
            if (emailType != null) {
                spec = spec.and(CustomerEmailSpecification.hasEmailType(emailType));
            }
            if (isPrimary != null) {
                spec = spec.and(CustomerEmailSpecification.isPrimary(isPrimary));
            }
            if (isActive != null) {
                spec = spec.and(CustomerEmailSpecification.isActive(isActive));
            }
            if (label != null && !label.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.labelLike(label));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(CustomerEmailSpecification.searchKeyword(keyword));
            }

            // Fetch paginated results
            Page<CustomerEmail> emailPage = customerEmailRepository.findAll(spec, pageable);

            // Convert to DTOs
            Page<CustomerEmailDTO> emailDTOPage = emailPage.map(this::convertToDTO);

            // Build response with pagination metadata
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("emails", emailDTOPage.getContent());
            responseData.put("currentPage", emailDTOPage.getNumber());
            responseData.put("totalItems", emailDTOPage.getTotalElements());
            responseData.put("totalPages", emailDTOPage.getTotalPages());
            responseData.put("pageSize", emailDTOPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Customer emails retrieved successfully",
                    responseData
                )
            );

        } catch (Exception e) {
            log.error("Error fetching customer emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch customer emails",
                    "CUSTOMER_EMAILS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert CustomerEmail entity to DTO
     */
    public CustomerEmailDTO convertToDTO(CustomerEmail email) {
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
