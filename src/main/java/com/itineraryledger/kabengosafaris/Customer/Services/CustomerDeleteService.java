package com.itineraryledger.kabengosafaris.Customer.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
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
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final SafariRepository safariRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final BookingInquiryRepository bookingInquiryRepository;

    @Autowired
    public CustomerDeleteService(
        CustomerRepository customerRepository,
        IdObfuscator idObfuscator,
        QuoteRepository quoteRepository,
        InvoiceRepository invoiceRepository,
        SafariRepository safariRepository,
        CreditNoteRepository creditNoteRepository,
        BookingInquiryRepository bookingInquiryRepository
    ) {
        this.customerRepository = customerRepository;
        this.idObfuscator = idObfuscator;
        this.quoteRepository = quoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.safariRepository = safariRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.bookingInquiryRepository = bookingInquiryRepository;
    }

    /**
     * Names of record types that still reference the customer, empty when deletable.
     * These FKs are what used to make hard deletes fail silently at the DB layer.
     */
    private List<String> blockingReferences(Long customerId) {
        List<String> refs = new ArrayList<>();
        if (quoteRepository.countByCustomerId(customerId) > 0) refs.add("quotes");
        if (invoiceRepository.countByCustomerId(customerId) > 0) refs.add("invoices");
        if (safariRepository.existsByCustomerId(customerId)) refs.add("safaris");
        if (creditNoteRepository.countByCustomerId(customerId) > 0) refs.add("credit notes");
        if (bookingInquiryRepository.existsByCustomerId(customerId)) refs.add("booking inquiries");
        return refs;
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
        List<String> deletedIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (Long id : ids) {
            String encodedId = idObfuscator.encodeId(id);
            try {
                Customer customer = customerRepository.findById(id).orElse(null);

                if (customer == null) {
                    skipped.add(Map.of("id", encodedId, "reason", "Customer not found"));
                    continue;
                }

                // Referential integrity: a referenced customer must be deactivated, not deleted.
                List<String> refs = blockingReferences(id);
                if (!refs.isEmpty()) {
                    log.warn("Customer {} referenced by {}, skipping deletion", customer.getCode(), refs);
                    skipped.add(Map.of(
                        "id", encodedId,
                        "code", customer.getCode(),
                        "reason", "Referenced by " + String.join(", ", refs) + " — deactivate instead"
                    ));
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((CustomerDeleteService) AopContext.currentProxy()).deleteCustomer(id);
                deletedIds.add(encodedId);
                log.info("Customer deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting customer: {}", id, e);
                skipped.add(Map.of("id", encodedId, "reason", "Delete failed unexpectedly"));
            }
        }

        String message = deletedIds.size() + " customer(s) deleted successfully";
        if (!skipped.isEmpty()) {
            message += ", " + skipped.size() + " skipped";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", deletedIds.size());
        data.put("deletedIds", deletedIds);
        data.put("skipped", skipped);

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                message,
                data
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
