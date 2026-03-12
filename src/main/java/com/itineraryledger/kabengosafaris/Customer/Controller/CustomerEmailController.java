package com.itineraryledger.kabengosafaris.Customer.Controller;

import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.CreateCustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerEmailDTOs.UpdateCustomerEmailDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices.CustomerEmailGetService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices.CreateCustomerEmailService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices.DeleteCustomerEmailService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices.UpdateCustomerEmailService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CustomerEmailController - REST controller for managing customer emails
 *
 * Provides endpoints for CRUD operations on customer emails with permission-based access control
 */
@RestController
@RequestMapping("/api/customer-emails")
@Slf4j
public class CustomerEmailController {

    private final CreateCustomerEmailService createCustomerEmailService;
    private final UpdateCustomerEmailService updateCustomerEmailService;
    private final DeleteCustomerEmailService deleteCustomerEmailService;
    private final CustomerEmailGetService customerEmailGetService;

    @Autowired
    public CustomerEmailController(
        CreateCustomerEmailService createCustomerEmailService,
        UpdateCustomerEmailService updateCustomerEmailService,
        DeleteCustomerEmailService deleteCustomerEmailService,
        CustomerEmailGetService customerEmailGetService
    ) {
        this.createCustomerEmailService = createCustomerEmailService;
        this.updateCustomerEmailService = updateCustomerEmailService;
        this.deleteCustomerEmailService = deleteCustomerEmailService;
        this.customerEmailGetService = customerEmailGetService;
    }

    /**
     * Create a new customer email
     *
     * @param createCustomerEmailDTO The email data
     * @return ResponseEntity with ApiResponse containing the created email
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_CUSTOMER_EMAIL')")
    public ResponseEntity<ApiResponse<?>> createCustomerEmail(
        @Valid @RequestBody CreateCustomerEmailDTO createCustomerEmailDTO
    ) {
        log.info("POST /api/customer-emails - Creating new email: {}", createCustomerEmailDTO.getEmail());
        return createCustomerEmailService.createCustomerEmail(createCustomerEmailDTO);
    }

    /**
     * Update an existing customer email
     *
     * @param idObfuscated The obfuscated email ID
     * @param updateCustomerEmailDTO The updated email data
     * @return ResponseEntity with ApiResponse containing the updated email
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER_EMAIL')")
    public ResponseEntity<ApiResponse<?>> updateCustomerEmail(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateCustomerEmailDTO updateCustomerEmailDTO
    ) {
        log.info("PUT /api/customer-emails/{} - Updating email", idObfuscated);
        return updateCustomerEmailService.updateCustomerEmail(idObfuscated, updateCustomerEmailDTO);
    }

    /**
     * Delete customer emails by list of IDs
     *
     * @param idObfuscatedList List of obfuscated email IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CUSTOMER_EMAIL')")
    public ResponseEntity<ApiResponse<?>> deleteCustomerEmails(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/customer-emails - Deleting {} emails", idObfuscatedList.size());
        return deleteCustomerEmailService.deleteCustomerEmails(idObfuscatedList);
    }

    /**
     * Get a single customer email by ID
     *
     * @param idObfuscated The obfuscated email ID
     * @return ResponseEntity with ApiResponse containing the email
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_EMAIL')")
    public ResponseEntity<ApiResponse<?>> getCustomerEmailById(
        @PathVariable String idObfuscated,
        @RequestParam(required = false) String scopeParentId
    ) {
        log.info("GET /api/customer-emails/{} - Fetching email by ID", idObfuscated);
        return customerEmailGetService.getCustomerEmailById(idObfuscated, scopeParentId);
    }

    /**
     * Get all customer emails with pagination, sorting, and filtering
     *
     * @param customerId Filter by customer ID (optional)
     * @param email Filter by email address (partial match)
     * @param emailType Filter by email type
     * @param isPrimary Filter by primary status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated emails
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_EMAIL')")
    public ResponseEntity<ApiResponse<?>> getAllCustomerEmails(
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) EmailType emailType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/customer-emails - Fetching all emails with filters");

        return customerEmailGetService.getAllCustomerEmails(
            customerId,
            email,
            emailType,
            isPrimary,
            isActive,
            label,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Get all emails for a specific customer
     *
     * @param customerId Required customer ID
     * @param email Filter by email address (partial match)
     * @param emailType Filter by email type
     * @param isPrimary Filter by primary status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated emails
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_EMAIL')")
    public ResponseEntity<ApiResponse<?>> getCustomersEmails(
        @PathVariable @NotBlank(message = "Customer ID is required") String customerId,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) EmailType emailType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/customer-emails/customer/{} - Fetching emails for customer", customerId);

        return customerEmailGetService.getCustomersEmails(
            customerId,
            email,
            emailType,
            isPrimary,
            isActive,
            label,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }
}
