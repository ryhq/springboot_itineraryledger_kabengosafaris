package com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs.CustomerDocumentDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs.UpdateCustomerDocumentDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for updating customer document metadata.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerDocumentUpdateService {

    private final CustomerDocumentRepository customerDocumentRepository;
    private final CustomerDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_CUSTOMER_DOCUMENT",
        description = "Updating customer document metadata",
        entityType = "CustomerDocument"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String obfuscatedId, UpdateCustomerDocumentDTO updateDTO) {
        log.info("Updating customer document with ID: {}", obfuscatedId);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscatedId);
            } catch (Exception e) {
                log.warn("Failed to decode customer document ID: {}", obfuscatedId, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            CustomerDocument document = customerDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Customer document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            if (updateDTO.getTitle() != null) {
                document.setTitle(updateDTO.getTitle());
            }
            if (updateDTO.getDocumentType() != null) {
                document.setDocumentType(updateDTO.getDocumentType().isBlank() ? null : com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType.valueOf(updateDTO.getDocumentType().trim()));
            }
            if (updateDTO.getDescription() != null) {
                document.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getDocumentNumber() != null) {
                document.setDocumentNumber(updateDTO.getDocumentNumber());
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

            document = customerDocumentRepository.save(document);

            CustomerDocumentDTO documentDTO = getService.toDTO(document);

            log.info("Customer document updated successfully: {}", id);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer document updated successfully", documentDTO)
            );

        } catch (Exception e) {
            log.error("Error updating customer document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update customer document", "DOCUMENT_UPDATE_FAILED")
            );
        }
    }
}
