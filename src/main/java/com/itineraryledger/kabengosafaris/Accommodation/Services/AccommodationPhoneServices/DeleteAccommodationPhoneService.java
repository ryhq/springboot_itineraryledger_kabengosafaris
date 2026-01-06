package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationPhoneServices;

import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationPhoneRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone;
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
 * DeleteAccommodationPhoneService - Service for deleting accommodation phones
 */
@Service
@Slf4j
@Transactional
public class DeleteAccommodationPhoneService {

    private final AccommodationPhoneRepository accommodationPhoneRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteAccommodationPhoneService(
        AccommodationPhoneRepository accommodationPhoneRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationPhoneRepository = accommodationPhoneRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete accommodation phones by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated phone IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteAccommodationPhones(List<String> idObfuscatedList) {
        log.info("Deleting {} accommodation phones", idObfuscatedList.size());

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

            return deleteAccommodationPhonesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting accommodation phones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete accommodation phones",
                    "ACCOMMODATION_PHONES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete accommodation phones by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteAccommodationPhonesInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                AccommodationPhone phone = accommodationPhoneRepository.findById(id).orElse(null);

                if (phone == null) {
                    log.warn("Accommodation phone not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((DeleteAccommodationPhoneService) AopContext.currentProxy()).deleteAccommodationPhone(id);
                deletedCount++;
                log.info("Accommodation phone deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting accommodation phone: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " accommodation phone(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(
        action = "DELETE_ACCOMMODATION_PHONE",
        description = "Deleting accommodation phone",
        entityType = "AccommodationPhone",
        entityIdParamName = "id"
    )
    public void deleteAccommodationPhone(Long id) {
        accommodationPhoneRepository.deleteById(id);
    }
}
