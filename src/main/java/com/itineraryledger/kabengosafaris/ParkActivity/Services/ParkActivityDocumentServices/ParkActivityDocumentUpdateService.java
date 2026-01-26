package com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.ParkActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.UpdateParkActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating park activity document metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ParkActivityDocumentUpdateService {

    private final ParkActivityDocumentRepository parkActivityDocumentRepository;
    private final ParkActivityDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_PARK_ACTIVITY_DOCUMENT",
        description = "Updating park activity document metadata",
        entityType = "ParkActivityDocument"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateParkActivityDocumentDTO updateDTO) {
        log.info("Updating park activity document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode park activity document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            ParkActivityDocument document = parkActivityDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            // Update fields if provided
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

            document = parkActivityDocumentRepository.save(document);

            ParkActivityDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Park activity document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activity document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating park activity document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update park activity document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
