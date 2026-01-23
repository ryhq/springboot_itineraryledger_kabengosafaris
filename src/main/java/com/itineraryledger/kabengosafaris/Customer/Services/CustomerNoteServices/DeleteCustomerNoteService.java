package com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerNoteRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote;
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
 * DeleteCustomerNoteService - Service for deleting customer notes
 */
@Service
@Slf4j
@Transactional
public class DeleteCustomerNoteService {

    private final CustomerNoteRepository customerNoteRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteCustomerNoteService(
        CustomerNoteRepository customerNoteRepository,
        IdObfuscator idObfuscator
    ) {
        this.customerNoteRepository = customerNoteRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete customer notes by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated note IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteCustomerNotes(List<String> idObfuscatedList) {
        log.info("Deleting {} customer notes", idObfuscatedList.size());

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

            return deleteCustomerNotesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting customer notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete customer notes",
                    "CUSTOMER_NOTES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete customer notes by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteCustomerNotesInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                CustomerNote note = customerNoteRepository.findById(id).orElse(null);

                if (note == null) {
                    log.warn("Customer note not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((DeleteCustomerNoteService) AopContext.currentProxy()).deleteCustomerNote(id);
                deletedCount++;
                log.info("Customer note deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting customer note: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " customer note(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_CUSTOMER_NOTE",
        description = "Deleting customer note",
        entityType = "CustomerNote",
        entityIdParamName = "id"
    )
    public void deleteCustomerNote(Long id) {
        customerNoteRepository.deleteById(id);
    }
}
