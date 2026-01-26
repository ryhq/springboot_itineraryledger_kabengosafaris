package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationDocumentRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for deleting accommodation documents.
 *
 * Provides:
 * - Bulk delete documents by list of IDs (permanently removes from database and filesystem)
 */
@Service
@Slf4j
@Transactional
public class AccommodationDocumentDeleteService {

    private final AccommodationDocumentRepository accommodationDocumentRepository;
    private final AccommodationDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public AccommodationDocumentDeleteService(
        AccommodationDocumentRepository accommodationDocumentRepository,
        AccommodationDocumentStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.accommodationDocumentRepository = accommodationDocumentRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Bulk delete accommodation documents by their IDs
     * Permanently removes from both database and filesystem
     *
     * @param obfuscatedIds List of obfuscated document IDs to delete
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> bulkDeleteDocuments(List<String> obfuscatedIds) {
        log.info("Deleting {} accommodation documents", obfuscatedIds != null ? obfuscatedIds.size() : 0);

        try {
            if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No document IDs provided",
                        "NO_IDS_PROVIDED"
                    )
                );
            }

            List<Long> documentIds = new ArrayList<>();
            List<String> notFoundIds = new ArrayList<>();
            int deletedCount = 0;

            // Decode all IDs first
            for (String obfuscatedId : obfuscatedIds) {
                try {
                    Long documentId = idObfuscator.decodeId(obfuscatedId);
                    documentIds.add(documentId);
                } catch (Exception e) {
                    log.warn("Failed to decode accommodation document ID: {}", obfuscatedId, e);
                    notFoundIds.add(obfuscatedId);
                }
            }

            // Delete each document
            for (Long documentId : documentIds) {
                AccommodationDocument document = accommodationDocumentRepository.findById(documentId).orElse(null);
                if (document != null) {
                    // Use proxy to ensure audit logging works
                    AccommodationDocumentDeleteService proxy = (AccommodationDocumentDeleteService) AopContext.currentProxy();
                    proxy.deleteSingleDocument(documentId, document.getFileName());
                    deletedCount++;
                } else {
                    notFoundIds.add(idObfuscator.encodeId(documentId));
                }
            }

            // Prepare response
            if (deletedCount > 0) {
                return ResponseEntity.ok().body(
                    ApiResponse.success(
                        200,
                        deletedCount + " accommodation document(s) deleted successfully",
                        null
                    )
                );
            } else {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "No accommodation documents were deleted. " + notFoundIds.size() + " document(s) not found",
                        "NO_DOCUMENTS_DELETED"
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error deleting accommodation documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete accommodation documents",
                    "ACCOMMODATION_DOCUMENT_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete a single accommodation document by ID (with audit logging)
     * Also removes the file from storage
     *
     * @param id The document ID
     * @param fileName The filename to delete from storage
     */
    @AuditLogAnnotation(
        action = "DELETE_ACCOMMODATION_DOCUMENT",
        description = "Deleting accommodation document",
        entityType = "AccommodationDocument",
        entityIdParamName = "id"
    )
    public void deleteSingleDocument(Long id, String fileName) {
        log.info("Deleting accommodation document with ID: {}", id);

        // Delete from database
        accommodationDocumentRepository.deleteById(id);

        // Delete file from storage
        boolean fileDeleted = storageService.deleteDocument(fileName);
        if (!fileDeleted) {
            log.warn("Document record deleted but file not found on disk: {}", fileName);
        }
    }
}
