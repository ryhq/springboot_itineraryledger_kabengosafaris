package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.PdfDocumentDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for updating PDF document types
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfDocumentUpdateService {

    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTemplateRepository pdfTemplateRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Toggle the enabled status of a PDF document type
     */
    @Transactional
    @AuditLogAnnotation(
        action = "PDF_DOCUMENT_TOGGLE_ENABLED",
        entityType = "PdfDocument",
        entityIdParamName = "idObfuscated",
        description = "Toggle PDF document enabled status"
    )
    public ResponseEntity<ApiResponse<?>> toggleEnabled(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            PdfDocument document = pdfDocumentRepository.findById(id).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document not found", "DOCUMENT_NOT_FOUND")
                );
            }

            // Toggle the enabled status
            boolean newStatus = !Boolean.TRUE.equals(document.getEnabled());
            document.setEnabled(newStatus);
            pdfDocumentRepository.save(document);

            log.info("PDF document '{}' enabled status toggled to: {}", document.getName(), newStatus);

            PdfDocumentDTO dto = mapToDTO(document);
            String message = newStatus ? "PDF document enabled successfully" : "PDF document disabled successfully";
            return ResponseEntity.ok(ApiResponse.success(200, message, dto));

        } catch (Exception e) {
            log.error("Error toggling PDF document enabled status: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to toggle PDF document status", "PDF_DOCUMENT_TOGGLE_FAILED")
            );
        }
    }

    /**
     * Map PdfDocument entity to DTO
     */
    private PdfDocumentDTO mapToDTO(PdfDocument document) {
        int templateCount = (int) pdfTemplateRepository.countByPdfDocumentId(document.getId());

        return PdfDocumentDTO.builder()
            .id(idObfuscator.encodeId(document.getId()))
            .name(document.getName())
            .displayName(document.getDisplayName())
            .description(document.getDescription())
            .dataSourceClass(document.getDataSourceClass())
            .rootVariableName(document.getRootVariableName())
            .enabled(document.getEnabled())
            .variablesJson(com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyVariableMerger
                .mergePdfVariables(document.getVariablesJson()))
            .templateCount(templateCount)
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }
}
