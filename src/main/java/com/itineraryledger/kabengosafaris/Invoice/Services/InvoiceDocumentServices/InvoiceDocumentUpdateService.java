package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.InvoiceDocumentDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.UpdateInvoiceDocumentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating invoice document metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceDocumentUpdateService {

    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final InvoiceDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_INVOICE_DOCUMENT",
        description = "Updating invoice document metadata",
        entityType = "InvoiceDocument",
        entityIdParamName = "obfuscatedId"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateInvoiceDocumentDTO updateDTO) {
        log.info("Updating invoice document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode invoice document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            InvoiceDocument document = invoiceDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice document not found", "DOCUMENT_NOT_FOUND")
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

            document = invoiceDocumentRepository.save(document);

            InvoiceDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Invoice document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating invoice document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update invoice document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
