package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationEmailServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationEmailRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail;
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
 * DeleteAccommodationEmailService - Service for deleting accommodation emails
 */
@Service
@Slf4j
@Transactional
public class DeleteAccommodationEmailService {

    private final AccommodationEmailRepository accommodationEmailRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteAccommodationEmailService(
        AccommodationEmailRepository accommodationEmailRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationEmailRepository = accommodationEmailRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete accommodation emails by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated email IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteAccommodationEmails(List<String> idObfuscatedList) {
        log.info("Deleting {} accommodation emails", idObfuscatedList.size());

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

            return deleteAccommodationEmailsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting accommodation emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete accommodation emails",
                    "ACCOMMODATION_EMAILS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete accommodation emails by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteAccommodationEmailsInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                AccommodationEmail email = accommodationEmailRepository.findById(id).orElse(null);

                if (email == null) {
                    log.warn("Accommodation email not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((DeleteAccommodationEmailService) AopContext.currentProxy()).deleteAccommodationEmail(id);
                deletedCount++;
                log.info("Accommodation email deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting accommodation email: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " accommodation email(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_ACCOMMODATION_EMAIL",
        description = "Deleting accommodation email",
        entityType = "AccommodationEmail",
        entityIdParamName = "id"
    )
    public void deleteAccommodationEmail(Long id) {
        accommodationEmailRepository.deleteById(id);
    }
}
