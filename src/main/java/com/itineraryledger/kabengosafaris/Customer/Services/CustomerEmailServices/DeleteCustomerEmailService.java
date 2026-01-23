package com.itineraryledger.kabengosafaris.Customer.Services.CustomerEmailServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
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
import java.util.List;

/**
 * DeleteCustomerEmailService - Service for deleting customer emails
 */
@Service
@Slf4j
@Transactional
public class DeleteCustomerEmailService {

    private final CustomerEmailRepository customerEmailRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteCustomerEmailService(
        CustomerEmailRepository customerEmailRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerEmailRepository = customerEmailRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete customer emails by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated email IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteCustomerEmails(List<String> idObfuscatedList) {
        log.info("Deleting {} customer emails", idObfuscatedList.size());

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

            return deleteCustomerEmailsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting customer emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete customer emails",
                    "CUSTOMER_EMAILS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete customer emails by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteCustomerEmailsInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                CustomerEmail email = customerEmailRepository.findById(id).orElse(null);

                if (email == null) {
                    log.warn("Customer email not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((DeleteCustomerEmailService) AopContext.currentProxy()).deleteCustomerEmail(id);
                deletedCount++;
                log.info("Customer email deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting customer email: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " customer email(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_CUSTOMER_EMAIL",
        description = "Deleting customer email",
        entityType = "CustomerEmail",
        entityIdParamName = "id"
    )
    public void deleteCustomerEmail(Long id) {
        customerEmailRepository.deleteById(id);
    }
}
