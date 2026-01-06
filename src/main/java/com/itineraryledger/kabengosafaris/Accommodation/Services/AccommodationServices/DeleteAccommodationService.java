package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
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
 * DeleteAccommodationService - Service for deleting accommodations
 *
 * Handles deletion of accommodations including:
 * - Single accommodation deletion
 * - Cascade deletion of all branches when deleting a parent
 * - Batch deletion of multiple accommodations
 */
@Service
@Slf4j
@Transactional
public class DeleteAccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeleteAccommodationService(
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete accommodations by list of obfuscated IDs
     * If an accommodation has branches, all branches will be deleted as well
     *
     * @param idObfuscatedList List of obfuscated accommodation IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteAccommodations(List<String> idObfuscatedList) {
        log.info("Deleting {} accommodations", idObfuscatedList.size());

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

            return deleteAccommodationsInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete accommodations",
                    "ACCOMMODATIONS_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete accommodations by list of IDs (internal method)
     * Includes cascade deletion of all branches
     */
    private ResponseEntity<ApiResponse<?>> deleteAccommodationsInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                Accommodation accommodation = accommodationRepository.findById(id).orElse(null);

                if (accommodation == null) {
                    log.warn("Accommodation not found: {}", id);
                    continue;
                }

                // Delete the accommodation and all its branches
                int branchesDeleted = deleteAccommodationWithBranches(accommodation);
                deletedCount += branchesDeleted;

                log.info("Accommodation and {} branch(es) deleted successfully: {}", branchesDeleted - 1, id);

            } catch (Exception e) {
                log.error("Error deleting accommodation: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " accommodation(s) deleted successfully",
                null
            )
        );
    }

    /**
     * Delete an accommodation and all its branches recursively
     *
     * @param accommodation The accommodation to delete
     * @return Total number of accommodations deleted (including branches)
     */
    private int deleteAccommodationWithBranches(Accommodation accommodation) {
        int deletedCount = 0;

        // If this accommodation has branches, delete them first (recursively)
        if (accommodation.getHasBranch() && accommodation.getBranches() != null && !accommodation.getBranches().isEmpty()) {
            log.info("Accommodation {} has {} branch(es), deleting them first",
                accommodation.getName(), accommodation.getBranches().size());

            // Create a copy of the branches list to avoid ConcurrentModificationException
            List<Accommodation> branchesToDelete = new ArrayList<>(accommodation.getBranches());

            for (Accommodation branch : branchesToDelete) {
                // Recursively delete each branch and its sub-branches
                deletedCount += deleteAccommodationWithBranches(branch);
            }
        }

        // Now delete the accommodation itself using AOP proxy for audit logging
        try {
            ((DeleteAccommodationService) AopContext.currentProxy()).deleteAccommodation(accommodation.getId());
            deletedCount++;
        } catch (Exception e) {
            log.error("Error deleting accommodation {}: {}", accommodation.getId(), e.getMessage());
        }

        return deletedCount;
    }

    /**
     * Delete a single accommodation by ID
     * This method is called with AOP proxy to trigger audit logging
     *
     * @param id The accommodation ID to delete
     */
    @AuditLogAnnotation(
        action = "DELETE_ACCOMMODATION",
        description = "Deleting accommodation",
        entityType = "Accommodation",
        entityIdParamName = "id"
    )
    public void deleteAccommodation(Long id) {
        accommodationRepository.deleteById(id);
    }
}
