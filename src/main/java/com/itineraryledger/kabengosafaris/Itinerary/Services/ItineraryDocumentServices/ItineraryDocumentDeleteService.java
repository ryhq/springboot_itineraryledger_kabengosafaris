package com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryDocumentRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for deleting itinerary documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ItineraryDocumentDeleteService {

    private final ItineraryDocumentRepository itineraryDocumentRepository;
    private final ItineraryDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "DELETE_ITINERARY_DOCUMENT",
        description = "Deleting itinerary document",
        entityType = "ItineraryDocument"
    )
    public ResponseEntity<ApiResponse<?>> deleteDocument(String obfuscatedId) {
        log.info("Deleting itinerary document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode itinerary document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            ItineraryDocument document = itineraryDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            String fileName = document.getFileName();

            itineraryDocumentRepository.delete(document);

            if (fileName != null && !fileName.isBlank()) {
                boolean fileDeleted = storageService.deleteDocument(fileName);
                if (!fileDeleted) {
                    log.warn("Failed to delete document file: {} - record already deleted", fileName);
                }
            }

            log.info("Itinerary document deleted successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Itinerary document deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting itinerary document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete itinerary document", "DOCUMENT_DELETE_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> deleteDocuments(List<String> obfuscatedIds) {
        log.info("Deleting {} itinerary documents", obfuscatedIds != null ? obfuscatedIds.size() : 0);

        try {
            if (obfuscatedIds == null || obfuscatedIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No document IDs provided", "NO_IDS_PROVIDED")
                );
            }

            List<String> deletedIds = new ArrayList<>();
            List<String> failedIds = new ArrayList<>();

            ItineraryDocumentDeleteService proxy = (ItineraryDocumentDeleteService) AopContext.currentProxy();

            for (String obfuscatedId : obfuscatedIds) {
                try {
                    ResponseEntity<ApiResponse<?>> response = proxy.deleteDocument(obfuscatedId);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        deletedIds.add(obfuscatedId);
                    } else {
                        failedIds.add(obfuscatedId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete document: {}", obfuscatedId, e);
                    failedIds.add(obfuscatedId);
                }
            }

            if (deletedIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(400, "Failed to delete any documents", "DELETE_FAILED")
                );
            }

            String message = String.format("%d document(s) deleted successfully", deletedIds.size());
            if (!failedIds.isEmpty()) {
                message += String.format(", %d failed", failedIds.size());
            }

            log.info("Bulk delete completed: {} deleted, {} failed", deletedIds.size(), failedIds.size());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, message, java.util.Map.of(
                    "deletedIds", deletedIds,
                    "failedIds", failedIds
                ))
            );

        } catch (Exception e) {
            log.error("Error deleting itinerary documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete itinerary documents", "BULK_DELETE_FAILED")
            );
        }
    }
}
