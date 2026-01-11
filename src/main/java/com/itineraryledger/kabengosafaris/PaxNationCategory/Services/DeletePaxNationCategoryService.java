package com.itineraryledger.kabengosafaris.PaxNationCategory.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
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
 * DeletePaxNationCategoryService - Service for deleting pax nation categories
 *
 * System categories (isSystem = true) cannot be deleted
 */
@Service
@Slf4j
public class DeletePaxNationCategoryService {

    private final PaxNationCategoryRepository paxNationCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeletePaxNationCategoryService(
        PaxNationCategoryRepository paxNationCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxNationCategoryRepository = paxNationCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete pax nation categories by list of IDs
     *
     * @param ids List of obfuscated category IDs
     * @return ResponseEntity with ApiResponse
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> deletePaxNationCategories(List<String> ids) {
        log.info("Deleting pax nation categories: {}", ids);

        try {
            // Validate input
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No pax nation category IDs provided", "NO_IDS_PROVIDED")
                );
            }

            // Decode all IDs and validate
            List<Long> decodedIds = new ArrayList<>();
            List<String> invalidIds = new ArrayList<>();

            for (String id : ids) {
                Long decodedId = idObfuscator.decodeId(id);
                if (decodedId == null) {
                    invalidIds.add(id);
                } else {
                    decodedIds.add(decodedId);
                }
            }

            if (!invalidIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid pax nation category ID(s): " + String.join(", ", invalidIds),
                        "INVALID_PAX_NATION_CATEGORY_IDS"
                    )
                );
            }

            // Check for system categories
            List<PaxNationCategory> categoriesToDelete = paxNationCategoryRepository.findAllById(decodedIds);
            List<String> systemCategoryNames = new ArrayList<>();

            for (PaxNationCategory category : categoriesToDelete) {
                if (category.isSystemCategory()) {
                    systemCategoryNames.add(category.getName());
                }
            }

            if (!systemCategoryNames.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot delete system pax nation categories: " + String.join(", ", systemCategoryNames),
                        "CANNOT_DELETE_SYSTEM_PAX_NATION_CATEGORIES"
                    )
                );
            }

            // Delete categories with audit logging
            int deletedCount = 0;
            DeletePaxNationCategoryService proxy = (DeletePaxNationCategoryService) AopContext.currentProxy();

            for (PaxNationCategory category : categoriesToDelete) {
                proxy.deleteSingleCategory(category);
                deletedCount++;
            }

            if (deletedCount == 0) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No pax nation categories were deleted (not found)",
                        "NO_PAX_NATION_CATEGORIES_DELETED"
                    )
                );
            }

            log.info("Deleted {} pax nation categories", deletedCount);

            return ResponseEntity.ok(
                ApiResponse.success(
                    200,
                    deletedCount + " pax nation category(ies) deleted successfully",
                    null
                )
            );

        } catch (Exception e) {
            log.error("Error deleting pax nation categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete pax nation categories: " + e.getMessage(),
                    "PAX_NATION_CATEGORY_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single category with audit logging
     */
    @AuditLogAnnotation(
        action = "DELETE_PAX_NATION_CATEGORY",
        description = "Deleting a pax nation category",
        entityType = "PaxNationCategory"
    )
    public void deleteSingleCategory(PaxNationCategory category) {
        log.info("Deleting pax nation category: {}", category.getName());
        paxNationCategoryRepository.delete(category);
    }
}
