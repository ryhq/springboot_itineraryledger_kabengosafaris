package com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs.ParkDocumentDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs.UpdateParkDocumentDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating park document metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ParkDocumentUpdateService {

    private final ParkDocumentRepository parkDocumentRepository;
    private final ParkDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_PARK_DOCUMENT",
        description = "Updating park document metadata",
        entityType = "ParkDocument"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateParkDocumentDTO updateDTO) {
        log.info("Updating park document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode park document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            ParkDocument document = parkDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park document not found", "DOCUMENT_NOT_FOUND")
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

            document = parkDocumentRepository.save(document);

            ParkDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Park document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating park document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update park document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
