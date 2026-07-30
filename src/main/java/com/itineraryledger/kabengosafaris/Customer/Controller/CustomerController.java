package com.itineraryledger.kabengosafaris.Customer.Controller;

import com.itineraryledger.kabengosafaris.Customer.DTOs.CreateCustomerDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.UpdateCustomerDTO;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerSource;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerCreateService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerDeleteService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerGetService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * CustomerController - REST controller for managing customers
 *
 * Endpoints:
 * - POST   /api/customers                    - Create customer
 * - GET    /api/customers                    - List with filters (supports code and keyword filtering)
 * - GET    /api/customers/{id}               - Get by ID
 * - PUT    /api/customers/{id}               - Update customer
 * - DELETE /api/customers                    - Delete (supports single and bulk)
 * - POST   /api/customers/{id}/deactivate    - Soft delete
 * - POST   /api/customers/{id}/reactivate    - Reactivate
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerCreateService createService;
    private final CustomerGetService getService;
    private final CustomerUpdateService updateService;
    private final CustomerDeleteService deleteService;

    // ========================
    // CREATE
    // ========================

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> createCustomer(
            @Valid @RequestBody CreateCustomerDTO createDTO
    ) {
        log.info("POST /api/customers - Creating new customer");
        return createService.createCustomer(createDTO);
    }

    // ========================
    // READ
    // ========================

    /**
     * Single customer. The optional filter/sort params are the ones the list page
     * was using, so next/previous navigation stays inside that same filtered set.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> getCustomerById(
            @PathVariable String id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) java.util.List<CustomerType> customerTypes,
            @RequestParam(required = false) java.util.List<CustomerSource> sources,
            @RequestParam(required = false) java.util.List<String> statuses,
            @RequestParam(required = false) java.util.List<String> flags,
            @RequestParam(required = false) java.util.List<String> qualities,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/customers/{} - Fetching customer", id);
        return getService.getCustomerById(
            id, keyword, country, customerTypes, sources, statuses, flags, qualities,
            createdAfter, sortBy, sortDirection
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> getAllCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) CustomerType customerType,
            @RequestParam(required = false) CustomerSource source,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isVip,
            @RequestParam(required = false) Boolean isBlacklisted,
            @RequestParam(required = false) Boolean hasBookings,
            @RequestParam(required = false) BigDecimal minTotalSpent,
            @RequestParam(required = false) BigDecimal maxTotalSpent,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdBefore,
            @RequestParam(required = false) Boolean hasEmail,
            @RequestParam(required = false) Boolean hasPhone,
            @RequestParam(required = false) Boolean passportExpiringSoon,
            @RequestParam(required = false) java.util.List<CustomerType> customerTypes,
            @RequestParam(required = false) java.util.List<CustomerSource> sources,
            @RequestParam(required = false) java.util.List<String> statuses,
            @RequestParam(required = false) java.util.List<String> flags,
            @RequestParam(required = false) java.util.List<String> qualities,
            @RequestParam(required = false, defaultValue = "true") Boolean includeStats,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/customers - Fetching all customers");
        return getService.getAllCustomers(
                name, email, phone, code, customerType, source,
                nationality, country, city, isActive, isVip, isBlacklisted,
                hasBookings, minTotalSpent, maxTotalSpent, keyword,
                createdAfter, createdBefore,
                hasEmail, hasPhone, passportExpiringSoon,
                customerTypes, sources, statuses, flags, qualities, includeStats,
                page, size, sortBy, sortDirection
        );
    }

    // ========================
    // UPDATE
    // ========================

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerDTO updateDTO
    ) {
        log.info("PUT /api/customers/{} - Updating customer", id);
        return updateService.updateCustomer(id, updateDTO);
    }

    // ========================
    // DELETE
    // ========================

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> deleteCustomers(
            @RequestBody List<String> ids
    ) {
        log.info("DELETE /api/customers - Deleting {} customers", ids.size());
        return deleteService.deleteCustomers(ids);
    }

    // ========================
    // SOFT DELETE / REACTIVATE
    // ========================

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> deactivateCustomer(
            @PathVariable String id
    ) {
        log.info("POST /api/customers/{}/deactivate - Deactivating customer", id);
        return deleteService.deactivateCustomer(id);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> reactivateCustomer(
            @PathVariable String id
    ) {
        log.info("POST /api/customers/{}/reactivate - Reactivating customer", id);
        return deleteService.reactivateCustomer(id);
    }
}
