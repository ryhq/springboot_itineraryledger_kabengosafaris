package com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs.ActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs.UpdateActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating activity document metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ActivityDocumentUpdateService {

    private final ActivityDocumentRepository activityDocumentRepository;
    private final ActivityDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_ACTIVITY_DOCUMENT",
        description = "Updating activity document metadata",
        entityType = "ActivityDocument"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateActivityDocumentDTO updateDTO) {
        log.info("Updating activity document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode activity document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            ActivityDocument document = activityDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Activity document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            if (updateDTO.getTitle() != null) {
                document.setTitle(updateDTO.getTitle());
            }
            if (updateDTO.getDocumentType() != null) {
                document.setDocumentType(updateDTO.getDocumentType());
            }
            if (updateDTO.getDescription() != null) {
                document.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getVersion() != null) {
                document.setVersion(updateDTO.getVersion());
            }
            if (updateDTO.getNotes() != null) {
                document.setNotes(updateDTO.getNotes());
            }
            if (updateDTO.getValidFrom() != null) {
                document.setValidFrom(updateDTO.getValidFrom());
            }
            if (updateDTO.getValidTo() != null) {
                document.setValidTo(updateDTO.getValidTo());
            }
            if (updateDTO.getIsActive() != null) {
                document.setIsActive(updateDTO.getIsActive());
            }

            document = activityDocumentRepository.save(document);

            ActivityDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Activity document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Activity document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating activity document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update activity document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
