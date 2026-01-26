package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs.AccommodationDocumentDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs.UpdateAccommodationDocumentDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationDocumentRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for updating accommodation document metadata.
 *
 * Note: This service updates metadata only (title, documentType, description, etc.)
 * To replace the actual document file, delete the old document and upload a new one.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AccommodationDocumentUpdateService {

    private final AccommodationDocumentRepository accommodationDocumentRepository;
    private final AccommodationDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    /**
     * Update an existing accommodation document's metadata
     *
     * @param obfuscatedId The obfuscated document ID
     * @param updateDTO The update DTO containing fields to update
     * @return ResponseEntity with ApiResponse containing updated document or error
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION_DOCUMENT",
        description = "Updating accommodation document metadata",
        entityType = "AccommodationDocument"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateAccommodationDocumentDTO updateDTO) {
        log.info("Updating accommodation document with ID: {}", obfuscatedId);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode accommodation document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            // Find document
            AccommodationDocument document = accommodationDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Accommodation document not found", "DOCUMENT_NOT_FOUND")
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

            // Save document
            document = accommodationDocumentRepository.save(document);

            // Convert to DTO
            AccommodationDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Accommodation document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Accommodation document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating accommodation document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update accommodation document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
