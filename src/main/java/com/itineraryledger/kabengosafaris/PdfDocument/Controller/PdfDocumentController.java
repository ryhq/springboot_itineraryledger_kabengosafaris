package com.itineraryledger.kabengosafaris.PdfDocument.Controller;

import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfDocumentGetService;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfDocumentUpdateService;
import com.itineraryledger.kabengosafaris.PdfDocument.Specification.PdfDocumentFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for PDF Document Types
 *
 * PDF Document types are built-in and cannot be created/deleted via API.
 * This controller provides read access and the ability to enable/disable document types.
 */
@RestController
@RequestMapping("/api/pdf-documents")
@RequiredArgsConstructor
@Slf4j
public class PdfDocumentController {

    private final PdfDocumentGetService getService;
    private final PdfDocumentUpdateService updateService;
    private final com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository documentRepository;
    private final com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    /**
     * Get all PDF document types
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PDF_DOCUMENT')")
    public ResponseEntity<ApiResponse<?>> getAllDocuments(
        @org.springframework.web.bind.annotation.ModelAttribute PdfDocumentFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/pdf-documents - Fetching all PDF document types");
        return getService.getAllDocuments(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * Get a PDF document type by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PDF_DOCUMENT')")
    public ResponseEntity<ApiResponse<?>> getDocumentById(
        @PathVariable String id,
        @org.springframework.web.bind.annotation.ModelAttribute PdfDocumentFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/pdf-documents/{} - Fetching PDF document type", id);
        return getService.getDocumentById(id, filter, sortBy, sortDirection);
    }

    /**
     * Get the variable schema for a document type
     */
    @GetMapping("/{id}/schema")
    @PreAuthorize("hasAuthority('PERM_READ_PDF_DOCUMENT')")
    public ResponseEntity<ApiResponse<?>> getDocumentSchema(@PathVariable String id) {
        log.info("GET /api/pdf-documents/{}/schema - Fetching document schema", id);
        return getService.getDocumentSchema(id);
    }

    /**
     * Toggle the enabled status of a PDF document type
     */
    /**
     * Switching a selection on or off in one request.
     *
     * The only thing about a document type anybody here owns. Off means the system produces
     * nothing of that kind when asked, so being able to do it deliberately across several at
     * once beats one at a time.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PDF_DOCUMENT')")
    public ResponseEntity<?> bulkUpdate(
        @org.springframework.web.bind.annotation.RequestBody
        com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("document type", documentRepository, request, document -> {
            if (request.getIsActive() != null) document.setEnabled(request.getIsActive());
        });
    }

    @PatchMapping("/{id}/toggle-enabled")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PDF_DOCUMENT')")
    public ResponseEntity<ApiResponse<?>> toggleEnabled(@PathVariable String id) {
        log.info("PATCH /api/pdf-documents/{}/toggle-enabled - Toggling enabled status", id);
        return updateService.toggleEnabled(id);
    }
}
