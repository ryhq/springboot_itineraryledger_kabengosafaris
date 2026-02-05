package com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.SafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.UpdateSafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariDocumentRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating safari document metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SafariDocumentUpdateService {

    private final SafariDocumentRepository safariDocumentRepository;
    private final SafariDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_SAFARI_DOCUMENT",
        description = "Updating safari document metadata",
        entityType = "SafariDocument"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateSafariDocumentDTO updateDTO) {
        log.info("Updating safari document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode safari document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            SafariDocument document = safariDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari document not found", "DOCUMENT_NOT_FOUND")
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

            document = safariDocumentRepository.save(document);

            SafariDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Safari document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating safari document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update safari document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
