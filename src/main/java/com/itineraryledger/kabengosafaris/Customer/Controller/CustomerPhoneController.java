package com.itineraryledger.kabengosafaris.Customer.Controller;

import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs.CreateCustomerPhoneDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerPhoneDTOs.UpdateCustomerPhoneDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices.CustomerPhoneGetService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices.CreateCustomerPhoneService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices.DeleteCustomerPhoneService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices.UpdateCustomerPhoneService;
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
 * CustomerPhoneController - REST controller for managing customer phones
 *
 * Provides endpoints for CRUD operations on customer phones with permission-based access control
 */
@RestController
@RequestMapping("/api/customer-phones")
@Slf4j
public class CustomerPhoneController {

    private final CreateCustomerPhoneService createCustomerPhoneService;
    private final UpdateCustomerPhoneService updateCustomerPhoneService;
    private final DeleteCustomerPhoneService deleteCustomerPhoneService;
    private final CustomerPhoneGetService customerPhoneGetService;

    @Autowired
    public CustomerPhoneController(
        CreateCustomerPhoneService createCustomerPhoneService,
        UpdateCustomerPhoneService updateCustomerPhoneService,
        DeleteCustomerPhoneService deleteCustomerPhoneService,
        CustomerPhoneGetService customerPhoneGetService
    ) {
        this.createCustomerPhoneService = createCustomerPhoneService;
        this.updateCustomerPhoneService = updateCustomerPhoneService;
        this.deleteCustomerPhoneService = deleteCustomerPhoneService;
        this.customerPhoneGetService = customerPhoneGetService;
    }

    /**
     * Create a new customer phone
     *
     * @param createCustomerPhoneDTO The phone data
     * @return ResponseEntity with ApiResponse containing the created phone
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_CUSTOMER_PHONE')")
    public ResponseEntity<ApiResponse<?>> createCustomerPhone(
        @Valid @RequestBody CreateCustomerPhoneDTO createCustomerPhoneDTO
    ) {
        log.info("POST /api/customer-phones - Creating new phone: {}", createCustomerPhoneDTO.getPhoneNumber());
        return createCustomerPhoneService.createCustomerPhone(createCustomerPhoneDTO);
    }

    /**
     * Update an existing customer phone
     *
     * @param idObfuscated The obfuscated phone ID
     * @param updateCustomerPhoneDTO The updated phone data
     * @return ResponseEntity with ApiResponse containing the updated phone
     */
    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER_PHONE')")
    public ResponseEntity<ApiResponse<?>> updateCustomerPhone(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateCustomerPhoneDTO updateCustomerPhoneDTO
    ) {
        log.info("PUT /api/customer-phones/{} - Updating phone", idObfuscated);
        return updateCustomerPhoneService.updateCustomerPhone(idObfuscated, updateCustomerPhoneDTO);
    }

    /**
     * Delete customer phones by list of IDs
     *
     * @param idObfuscatedList List of obfuscated phone IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CUSTOMER_PHONE')")
    public ResponseEntity<ApiResponse<?>> deleteCustomerPhones(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/customer-phones - Deleting {} phones", idObfuscatedList.size());
        return deleteCustomerPhoneService.deleteCustomerPhones(idObfuscatedList);
    }

    /**
     * Get a single customer phone by ID
     *
     * @param idObfuscated The obfuscated phone ID
     * @return ResponseEntity with ApiResponse containing the phone
     */
    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_PHONE')")
    public ResponseEntity<ApiResponse<?>> getCustomerPhoneById(
        @PathVariable String idObfuscated,
        @RequestParam(required = false) String scopeParentId,
        // the list's filter context, so prev/next walks the set the user was in
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) java.util.List<com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType> phoneTypes,
        @RequestParam(required = false) java.util.List<String> statuses,
        @RequestParam(required = false) java.util.List<String> qualities,
        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/customer-phones/{} - Fetching phone by ID", idObfuscated);
        return customerPhoneGetService.getCustomerPhoneById(
            idObfuscated, scopeParentId, keyword, phoneTypes, statuses, qualities, createdAfter, sortBy, sortDirection
        );
    }

    /**
     * Get all customer phones with pagination, sorting, and filtering
     *
     * @param customerId Filter by customer ID (optional)
     * @param phoneNumber Filter by phone number (partial match)
     * @param phoneType Filter by phone type
     * @param isPrimary Filter by primary status
     * @param isWhatsApp Filter by WhatsApp status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated phones
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_PHONE')")
    public ResponseEntity<ApiResponse<?>> getAllCustomerPhones(
        @RequestParam(required = false) String customerId,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam(required = false) PhoneType phoneType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isWhatsApp,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ,
            @RequestParam(required = false) java.util.List<com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType> phoneTypes,
            @RequestParam(required = false) java.util.List<String> statuses,
            @RequestParam(required = false) java.util.List<String> qualities,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdBefore,
            @RequestParam(required = false) Boolean includeStats) {
        log.info("GET /api/customer-phones - Fetching all phones with filters");

        return customerPhoneGetService.getAllCustomerPhones(
            customerId,
            phoneNumber,
            phoneType,
            isPrimary,
            isWhatsApp,
            isActive,
            label,
            keyword,
            phoneTypes,
            statuses,
            qualities,
            createdAfter,
            createdBefore,
            includeStats,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Get all phones for a specific customer
     *
     * @param customerId Required customer ID
     * @param phoneNumber Filter by phone number (partial match)
     * @param phoneType Filter by phone type
     * @param isPrimary Filter by primary status
     * @param isWhatsApp Filter by WhatsApp status
     * @param isActive Filter by active status
     * @param label Filter by label (partial match)
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated phones
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_PHONE')")
    public ResponseEntity<ApiResponse<?>> getCustomersPhones(
        @PathVariable @NotBlank(message = "Customer ID is required") String customerId,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam(required = false) PhoneType phoneType,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isWhatsApp,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String label,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/customer-phones/customer/{} - Fetching phones for customer", customerId);

        return customerPhoneGetService.getCustomersPhones(
            customerId,
            phoneNumber,
            phoneType,
            isPrimary,
            isWhatsApp,
            isActive,
            label,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Customer.Repository.CustomerPhoneRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Only the flags present in the body apply, so the same endpoint serves
     * activate, deactivate. Returns per-id
     * outcomes rather than a bare 200 that hides what did not change.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("customer phone", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
