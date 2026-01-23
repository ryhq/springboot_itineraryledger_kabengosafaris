package com.itineraryledger.kabengosafaris.Customer.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomerDeleteService - Service for deleting customers
 */
@Service
@Slf4j
@Transactional
public class CustomerDeleteService {

    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CustomerDeleteService(
        CustomerRepository customerRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerRepository = customerRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete customers by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated customer IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteCustomers(List<String> idObfuscatedList) {
        log.info("Deleting {} customers", idObfuscatedList.size());

        if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No customer IDs provided", "NO_IDS_PROVIDED")
            );
        }

        try {
            // Decode all obfuscated IDs
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteCustomersInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting customers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete customers",
                    "CUSTOMERS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete customers by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteCustomersInternal(List<Long> ids) {
        int deletedCount = 0;
        List<String> skippedWithBookings = new ArrayList<>();

        for (Long id : ids) {
            try {
                Customer customer = customerRepository.findById(id).orElse(null);

                if (customer == null) {
                    log.warn("Customer not found: {}", id);
                    continue;
                }

                // Check if customer has bookings
                if (customer.getTotalBookings() != null && customer.getTotalBookings() > 0) {
                    log.warn("Customer {} has bookings, skipping deletion", customer.getCode());
                    skippedWithBookings.add(customer.getCode());
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((CustomerDeleteService) AopContext.currentProxy()).deleteCustomer(id);
                deletedCount++;
                log.info("Customer deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting customer: {}", id, e);
            }
        }

        String message = deletedCount + " customer(s) deleted successfully";
        if (!skippedWithBookings.isEmpty()) {
            message += ". " + skippedWithBookings.size() + " customer(s) skipped due to existing bookings: " + String.join(", ", skippedWithBookings);
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                message,
                null
            )
        );
    }

    @AuditLogAnnotation(action = "DELETE_CUSTOMER", description = "Deleting customer", entityType = "Customer", entityIdParamName = "id")
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    /**
     * Soft delete (deactivate) a customer
     */
    @AuditLogAnnotation(action = "DEACTIVATE_CUSTOMER", description = "Deactivating a customer", entityType = "Customer")
    public ResponseEntity<ApiResponse<?>> deactivateCustomer(String idObfuscated) {
        log.info("Deactivating customer with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            Customer customer = customerRepository.findById(id).orElse(null);
            if (customer == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Customer not found", "CUSTOMER_NOT_FOUND")
                );
            }

            customer.setIsActive(false);
            customerRepository.save(customer);

            log.info("Customer deactivated: {}", customer.getCode());

            Map<String, Object> response = new HashMap<>();
            response.put("id", idObfuscated);
            response.put("code", customer.getCode());
            response.put("isActive", false);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer deactivated successfully", response)
            );

        } catch (Exception e) {
            log.error("Error deactivating customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to deactivate customer", "CUSTOMER_DEACTIVATE_FAILED")
            );
        }
    }

    /**
     * Reactivate a customer
     */
    @AuditLogAnnotation(action = "REACTIVATE_CUSTOMER", description = "Reactivating a customer", entityType = "Customer")
    public ResponseEntity<ApiResponse<?>> reactivateCustomer(String idObfuscated) {
        log.info("Reactivating customer with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            Customer customer = customerRepository.findById(id).orElse(null);
            if (customer == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Customer not found", "CUSTOMER_NOT_FOUND")
                );
            }

            customer.setIsActive(true);
            customerRepository.save(customer);

            log.info("Customer reactivated: {}", customer.getCode());

            Map<String, Object> response = new HashMap<>();
            response.put("id", idObfuscated);
            response.put("code", customer.getCode());
            response.put("isActive", true);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer reactivated successfully", response)
            );

        } catch (Exception e) {
            log.error("Error reactivating customer", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reactivate customer", "CUSTOMER_REACTIVATE_FAILED")
            );
        }
    }
}
