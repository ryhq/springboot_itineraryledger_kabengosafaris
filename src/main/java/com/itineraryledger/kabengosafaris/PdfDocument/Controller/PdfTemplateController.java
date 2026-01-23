package com.itineraryledger.kabengosafaris.PdfDocument.Controller;

import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.CreatePdfTemplateDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.UpdatePdfTemplateDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.*;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for PDF Templates
 *
 * Provides CRUD operations for PDF templates within document types.
 */
@RestController
@RequestMapping("/api/pdf-templates")
@RequiredArgsConstructor
@Slf4j
public class PdfTemplateController {

    private final PdfTemplateCreateService createService;
    private final PdfTemplateGetService getService;
    private final PdfTemplateUpdateService updateService;
    private final PdfTemplateDeleteService deleteService;
    private final PdfGenerationService generationService;

    // ========================
    // CREATE
    // ========================

    /**
     * Create a new PDF template for a document type
     */
    @PostMapping("/document/{documentId}")
    @PreAuthorize("hasAuthority('PERM_CREATE_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> createTemplate(
        @PathVariable String documentId,
        @Valid @RequestBody CreatePdfTemplateDTO createDTO
    ) {
        log.info("POST /api/pdf-templates/document/{} - Creating template: {}", documentId, createDTO.getName());
        return createService.createTemplate(documentId, createDTO);
    }

    // ========================
    // READ
    // ========================

    /**
     * Get all templates with filtering and pagination
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> getAllTemplates(
        @RequestParam(required = false) String documentId,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) Boolean isDefault,
        @RequestParam(required = false) Boolean isSystemDefault,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String paperSize,
        @RequestParam(required = false) String orientation,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/pdf-templates - Fetching templates with filters");
        return getService.getAllTemplates(documentId, enabled, isDefault, isSystemDefault, name, paperSize, orientation, page, size, sortDirection);
    }

    /**
     * Get a template by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> getTemplateById(@PathVariable String id) {
        log.info("GET /api/pdf-templates/{} - Fetching template", id);
        return getService.getTemplateById(id);
    }

    /**
     * Get template content (HTML)
     */
    @GetMapping("/{id}/content")
    @PreAuthorize("hasAuthority('PERM_READ_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> getTemplateContent(@PathVariable String id) {
        log.info("GET /api/pdf-templates/{}/content - Fetching template content", id);
        return getService.getTemplateContent(id);
    }

    // ========================
    // UPDATE
    // ========================

    /**
     * Update a template
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> updateTemplate(
        @PathVariable String id,
        @Valid @RequestBody UpdatePdfTemplateDTO updateDTO
    ) {
        log.info("PUT /api/pdf-templates/{} - Updating template", id);
        return updateService.updateTemplate(id, updateDTO);
    }

    /**
     * Restore a system default template to original content
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> restoreSystemDefault(@PathVariable String id) {
        log.info("POST /api/pdf-templates/{}/restore - Restoring system default template", id);
        return updateService.restoreSystemDefault(id);
    }

    // ========================
    // DELETE
    // ========================

    /**
     * Delete multiple templates (bulk delete)
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> deleteTemplates(@RequestBody List<String> ids) {
        log.info("DELETE /api/pdf-templates - Deleting {} templates", ids.size());
        return deleteService.deleteTemplates(ids);
    }

    /**
     * Delete a single template
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> deleteTemplate(@PathVariable String id) {
        log.info("DELETE /api/pdf-templates/{} - Deleting template", id);
        return deleteService.deleteTemplate(id);
    }

    // ========================
    // PDF GENERATION
    // ========================

    /**
     * Generate PDF using a specific template
     * Returns the PDF file as binary download
     */
    @PostMapping("/{templateId}/generate")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<?> generatePdfWithTemplate(
        @PathVariable String templateId,
        @RequestParam String documentType,
        @RequestParam String dataId
    ) {
        log.info("POST /api/pdf-templates/{}/generate - Generating PDF for {} / {}", templateId, documentType, dataId);
        return generationService.generatePdf(documentType, dataId, templateId);
    }

    /**
     * Preview PDF (returns rendered HTML for preview)
     */
    @PostMapping("/{templateId}/preview")
    @PreAuthorize("hasAuthority('PERM_GENERATE_PDF')")
    public ResponseEntity<ApiResponse<?>> previewPdf(
        @PathVariable String templateId,
        @RequestParam String documentType,
        @RequestParam String dataId
    ) {
        log.info("POST /api/pdf-templates/{}/preview - Preview PDF for {} / {}", templateId, documentType, dataId);
        return generationService.previewPdf(documentType, dataId, templateId);
    }
}
