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
    private final com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository templateRepository;
    private final com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;
    private final com.itineraryledger.kabengosafaris.Security.IdObfuscator idObfuscator;
    private final PdfTemplateStorageService storageService;

    // ========================
    // CREATE
    // ========================

    /**
     * Create a new PDF template for a document type
     */
    /**
     * Create a template, with the document named in the body.
     *
     * The nested path stays exactly as it was; this exists so a template can be created
     * from the flat list as well as from inside a document, and so the generic create form
     * has one endpoint it can post to.
     *
     * `content` may be left out, and usually should be. A PDF layout is a whole HTML
     * document — asking somebody to paste one into a create form is not a feature — so an
     * empty one starts as a copy of whatever that document renders with today. You get a
     * working layout to edit rather than a blank page, which is what "add a template"
     * actually means to the person asking.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> createTemplateFlat(@RequestBody CreateTemplateRequest request) {
        if (request == null || request.getDocumentId() == null || request.getDocumentId().isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Which document is this template for? documentId is required",
                    "DOCUMENT_ID_REQUIRED"));
        }

        CreatePdfTemplateDTO dto = new CreatePdfTemplateDTO();
        dto.setName(request.getName());
        dto.setDescription(request.getDescription());
        dto.setVersion(request.getVersion());
        dto.setIsDefault(request.getIsDefault());
        dto.setEnabled(request.getEnabled());
        dto.setMarginTop(request.getMarginTop());
        dto.setMarginBottom(request.getMarginBottom());
        dto.setMarginLeft(request.getMarginLeft());
        dto.setMarginRight(request.getMarginRight());
        if (request.getPaperSize() != null && !request.getPaperSize().isBlank()) {
            dto.setPaperSize(com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize
                .valueOf(request.getPaperSize().toUpperCase()));
        }
        if (request.getOrientation() != null && !request.getOrientation().isBlank()) {
            dto.setOrientation(com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation
                .valueOf(request.getOrientation().toUpperCase()));
        }

        String content = request.getContent();
        if (content == null || content.isBlank()) {
            content = startingContentFor(request.getDocumentId());
            if (content == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "This document has no existing layout to copy, so a new template needs its "
                            + "content supplied",
                        "NO_STARTING_CONTENT"));
            }
        }
        dto.setContent(content);

        return createService.createTemplate(request.getDocumentId(), dto);
    }

    /**
     * The layout a new template starts from: whatever this document renders with today,
     * falling back to the version that shipped.
     */
    private String startingContentFor(String documentIdObfuscated) {
        try {
            Long documentId = idObfuscator.decodeId(documentIdObfuscated);
            var source = templateRepository
                .findByPdfDocumentIdAndIsDefaultAndEnabled(documentId, true, true)
                .or(() -> templateRepository.findByPdfDocumentIdAndIsSystemDefault(documentId, true))
                .orElse(null);
            if (source == null) return null;
            return storageService.readTemplateFile(source.getFileName());
        } catch (Exception e) {
            log.warn("Could not read a starting layout for document {}", documentIdObfuscated, e);
            return null;
        }
    }

    /** CreatePdfTemplateDTO plus the document it belongs to, with enums as plain strings. */
    @lombok.Data
    public static class CreateTemplateRequest {
        private String documentId;
        private String name;
        private String description;
        private String content;
        private String paperSize;
        private String orientation;
        private Integer marginTop;
        private Integer marginBottom;
        private Integer marginLeft;
        private Integer marginRight;
        private Boolean isDefault;
        private Boolean enabled;
        private String version;
    }

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
     *
     * @param documentId Filter by obfuscated PdfDocument ID
     * @param documentType Filter by PdfDocument name (e.g., "FULL_QUOTE", "FULL_ITINERARY")
     * @param rootVariableName Filter by PdfDocument root variable name (e.g., "quote", "itinerary")
     * @param enabled Filter by enabled status
     * @param isDefault Filter by default status
     * @param isSystemDefault Filter by system default status
     * @param name Filter by template name (partial match)
     * @param paperSize Filter by paper size
     * @param orientation Filter by orientation
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> getAllTemplates(
        @RequestParam(required = false) String documentId,
        @RequestParam(required = false) String documentType,
        @RequestParam(required = false) String rootVariableName,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) Boolean isDefault,
        @RequestParam(required = false) Boolean isSystemDefault,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String paperSize,
        @RequestParam(required = false) String orientation,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestParam(required = false) Boolean includeStats
    ) {
        log.info("GET /api/pdf-templates - Fetching templates with filters");
        return getService.getAllTemplates(documentId, documentType, rootVariableName, enabled, isDefault, isSystemDefault, name, paperSize, orientation, page, size, sortBy, sortDirection, includeStats);
    }

    /**
     * Get a template by ID
     */
    /**
     * Enabling or disabling a selection in one request.
     *
     * Only `enabled` is offered. Which template a document is rendered WITH is a choice
     * between siblings — turning several on at once cannot express it, and a bulk control
     * that silently picked a winner would be worse than not having one.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PDF_TEMPLATE')")
    public ResponseEntity<?> bulkUpdate(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("template", templateRepository, request, template -> {
            if (request.getIsActive() != null) template.setEnabled(request.getIsActive());
        });
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PDF_TEMPLATE')")
    public ResponseEntity<ApiResponse<?>> getTemplateById(
        @PathVariable String id,
        @RequestParam(required = false) String documentId,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/pdf-templates/{} - Fetching template", id);
        return getService.getTemplateById(id, documentId, sortBy, sortDirection);
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

    /**
     * Restore every system-default pdf templates at once.
     *
     * The one action that makes a shipped template fix real: seeded files are never overwritten, so
     * without this a correction sits in the release and nothing changes.
     */
    @PostMapping("/restore-defaults")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PDF_TEMPLATE')")
    public ResponseEntity<?> restoreAllSystemDefaults() {
        log.info("POST /api/pdf-templates/restore-defaults - restoring every system default");
        return updateService.restoreAllSystemDefaults();
    }
}
