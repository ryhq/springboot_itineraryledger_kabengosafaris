package com.itineraryledger.kabengosafaris.Customer.Services.CustomerPhoneServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerPhoneRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
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
 * DeleteCustomerPhoneService - Service for deleting customer phones
 */
@Service
@Slf4j
@Transactional
public class DeleteCustomerPhoneService {

    private final CustomerPhoneRepository customerPhoneRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteCustomerPhoneService(
        CustomerPhoneRepository customerPhoneRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerPhoneRepository = customerPhoneRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete customer phones by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated phone IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteCustomerPhones(List<String> idObfuscatedList) {
        log.info("Deleting {} customer phones", idObfuscatedList.size());

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

            return deleteCustomerPhonesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting customer phones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete customer phones",
                    "CUSTOMER_PHONES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete customer phones by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteCustomerPhonesInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                CustomerPhone phone = customerPhoneRepository.findById(id).orElse(null);

                if (phone == null) {
                    log.warn("Customer phone not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((DeleteCustomerPhoneService) AopContext.currentProxy()).deleteCustomerPhone(id);
                deletedCount++;
                log.info("Customer phone deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting customer phone: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " customer phone(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_CUSTOMER_PHONE",
        description = "Deleting customer phone",
        entityType = "CustomerPhone",
        entityIdParamName = "id"
    )
    public void deleteCustomerPhone(Long id) {
        customerPhoneRepository.deleteById(id);
    }
}
