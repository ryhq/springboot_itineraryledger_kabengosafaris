package com.itineraryledger.kabengosafaris.PaxAgeCategory.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
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
 * DeletePaxAgeCategoryService - Service for deleting pax age categories
 *
 * IMPORTANT PROTECTION RULE:
 * System pax age categories cannot be deleted (created by initializer).
 * User-created pax age categories can be deleted.
 */
@Service
@Slf4j
@Transactional
public class DeletePaxAgeCategoryService {

    private final PaxAgeCategoryRepository paxAgeCategoryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public DeletePaxAgeCategoryService(
        PaxAgeCategoryRepository paxAgeCategoryRepository,
        IdObfuscator idObfuscator
    ) {
        this.paxAgeCategoryRepository = paxAgeCategoryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete pax age categories by list of IDs
     * System categories cannot be deleted - only user-created categories can be deleted
     *
     * @param idObfuscatedList List of obfuscated category IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deletePaxAgeCategories(List<String> idObfuscatedList) {
        log.info("Deleting {} pax age categories", idObfuscatedList.size());

        try {
            if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No pax age category IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> categoryIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long categoryId = idObfuscator.decodeId(idObfuscated);
                    categoryIds.add(categoryId);
                } catch (Exception e) {
                    log.warn("Failed to decode pax age category ID: {}", idObfuscated, e);
                    notFoundIds.add(idObfuscated);
                }
            }

            // First, validate that no category in the list is system
            List<String> systemCategoryIds = new ArrayList<>();

            for (Long categoryId : categoryIds) {
                PaxAgeCategory category = paxAgeCategoryRepository.findById(categoryId).orElse(null);
                if (category != null && category.getIsSystem() != null && category.getIsSystem()) {
                    systemCategoryIds.add(idObfuscator.encodeId(categoryId));
                }
            }

            // If any system categories found, reject entire operation
            if (!systemCategoryIds.isEmpty()) {
                log.warn("Cannot delete: {} pax age category(ies) in the list are system category(ies)", systemCategoryIds.size());

                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Cannot delete any pax age categories: " + systemCategoryIds.size() + " category(ies) in the list are system categories created by initializer",
                        "CANNOT_DELETE_SYSTEM_PAX_AGE_CATEGORIES"
                    )
                );
            }

            // Delete each category
            for (Long categoryId : categoryIds) {
                PaxAgeCategory category = paxAgeCategoryRepository.findById(categoryId).orElse(null);
                if (category != null) {
                    // Use proxy to ensure audit logging works
                    DeletePaxAgeCategoryService proxy = (DeletePaxAgeCategoryService) AopContext.currentProxy();
                    proxy.deleteSinglePaxAgeCategory(categoryId);
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(categoryId));
                }
            }

            // Prepare response
            if (deletedCount > 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(
                        200,
                        deletedCount + " pax age category(ies) deleted successfully",
                        null
                    )
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No pax age categories were deleted. " + notFoundIds.size() + " category(ies) not found",
                        "NO_PAX_AGE_CATEGORIES_DELETED"
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error deleting pax age categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete pax age categories",
                    "PAX_AGE_CATEGORY_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single pax age category by ID (with audit logging)
     */
    @AuditLogAnnotation(
        action = "DELETE_PAX_AGE_CATEGORY",
        description = "Deleting pax age category",
        entityType = "PaxAgeCategory",
        entityIdParamName = "id"
    )
    public void deleteSinglePaxAgeCategory(Long id) {
        log.info("Deleting pax age category with ID: {}", id);
        paxAgeCategoryRepository.deleteById(id);
    }
}
