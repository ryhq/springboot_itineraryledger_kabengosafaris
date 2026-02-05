package com.itineraryledger.kabengosafaris.Quote.Services.QuoteDocumentServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs.QuoteDocumentDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs.UpdateQuoteDocumentDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteDocument;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating quote document metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteDocumentUpdateService {

    private final QuoteDocumentRepository quoteDocumentRepository;
    private final QuoteDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_QUOTE_DOCUMENT",
        description = "Updating quote document metadata",
        entityType = "QuoteDocument",
        entityIdParamName = "obfuscatedId"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateQuoteDocumentDTO updateDTO) {
        log.info("Updating quote document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode quote document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            QuoteDocument document = quoteDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quote document not found", "DOCUMENT_NOT_FOUND")
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

            document = quoteDocumentRepository.save(document);

            QuoteDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Quote document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quote document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating quote document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update quote document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
