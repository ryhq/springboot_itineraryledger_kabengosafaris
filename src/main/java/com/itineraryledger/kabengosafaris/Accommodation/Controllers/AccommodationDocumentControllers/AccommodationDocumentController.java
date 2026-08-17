package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationDocumentControllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ContentTypes;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs.UpdateAccommodationDocumentDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDocumentDTOs.UploadAccommodationDocumentsDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationDocumentRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices.AccommodationDocumentCreateService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices.AccommodationDocumentDeleteService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices.AccommodationDocumentGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices.AccommodationDocumentStorageService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationDocumentServices.AccommodationDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Accommodation Document Management
 *
 * Provides endpoints to:
 * - List documents with filters, pagination, and sorting
 * - Get document by ID
 * - Get document file by filename
 * - Serve document file by ID
 * - Upload multiple documents
 * - Update document metadata
 * - Bulk delete documents
 */
@RestController
@RequestMapping("/api/accommodation-documents")
@Validated
public class AccommodationDocumentController {

    private final AccommodationDocumentGetService getService;
    private final AccommodationDocumentCreateService createService;
    private final AccommodationDocumentUpdateService updateService;
    private final AccommodationDocumentDeleteService deleteService;
    private final AccommodationDocumentStorageService storageService;
    private final AccommodationDocumentRepository accommodationDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public AccommodationDocumentController(
        AccommodationDocumentGetService getService,
        AccommodationDocumentCreateService createService,
        AccommodationDocumentUpdateService updateService,
        AccommodationDocumentDeleteService deleteService,
        AccommodationDocumentStorageService storageService,
        AccommodationDocumentRepository accommodationDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.accommodationDocumentRepository = accommodationDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/accommodation-documents
     *
     * Always sorts by createdAt, descending by default.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "accommodationId", required = false) String accommodationId,
            @RequestParam(value = "accommodationName", required = false) String accommodationName,
            @RequestParam(value = "accommodationType", required = false) AccommodationType accommodationType,
            @RequestParam(value = "accommodationCategory", required = false) AccommodationCategory accommodationCategory,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "statuses", required = false) java.util.List<String> statuses,
            @RequestParam(value = "validity", required = false) java.util.List<String> validity,
            @RequestParam(value = "createdAfter", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.LocalDateTime createdAfter,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "includeStats", required = false) Boolean includeStats,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        return getService.getAllDocuments(
            accommodationId,
            accommodationName,
            accommodationType,
            accommodationCategory,
            documentType,
            title,
            version,
            isActive,
            currentlyValid,
            statuses,
            validity,
            createdAfter,
            keyword,
            includeStats,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Get document by ID
     * GET /api/accommodation-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(
            @PathVariable("id") String id,
            @RequestParam(value = "scopeParentId", required = false) String scopeParentId
    ) {
        return getService.getDocumentById(id, scopeParentId);
    }

    /**
     * Get document file by filename
     * GET /api/accommodation-documents/file/{fileName}
     *
     * Returns the actual document file as a resource.
     * Example: GET /api/accommodation-documents/file/f2e5a046548d723b_1701304567890.pdf
     *
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        // Find document by filename
        AccommodationDocument document = getService.getDocumentByFileName(fileName);
        if (document == null || !document.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        // Read document bytes from storage
        byte[] documentBytes = storageService.readDocumentBytes(document.getFileName());
        if (documentBytes == null) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
        String mimeType = document.getFileType() != null
            ? document.getFileType()
            : storageService.getMimeType(document.getFileName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ContentTypes.safe(mimeType));
        headers.setContentLength(documentBytes.length);
        headers.setCacheControl("public, max-age=86400");

        // Set content disposition for download (use inline for PDFs to view in browser)
        String disposition = mimeType.equals("application/pdf") ? "inline" : "attachment";
        headers.setContentDisposition(
            ("attachment".equalsIgnoreCase(disposition)
                ? org.springframework.http.ContentDisposition.attachment()
                : org.springframework.http.ContentDisposition.inline())
                .filename(document.getFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
    }

    /**
     * Serve actual document file by obfuscated ID
     * GET /api/accommodation-documents/{id}/file
     *
     * This endpoint serves the actual document bytes for display/download.
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getDocumentFile(@PathVariable("id") String id) {
        // Decode document ID
        Long documentId;
        try {
            documentId = idObfuscator.decodeId(id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        // Find document
        AccommodationDocument document = accommodationDocumentRepository.findById(documentId).orElse(null);
        if (document == null || !document.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        // Read document bytes from storage
        byte[] documentBytes = storageService.readDocumentBytes(document.getFileName());
        if (documentBytes == null) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
        String mimeType = document.getFileType() != null
            ? document.getFileType()
            : storageService.getMimeType(document.getFileName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ContentTypes.safe(mimeType));
        headers.setContentLength(documentBytes.length);
        headers.setCacheControl("public, max-age=86400");

        // Set content disposition for download (use inline for PDFs to view in browser)
        String disposition = mimeType.equals("application/pdf") ? "inline" : "attachment";
        headers.setContentDisposition(
            ("attachment".equalsIgnoreCase(disposition)
                ? org.springframework.http.ContentDisposition.attachment()
                : org.springframework.http.ContentDisposition.inline())
                .filename(document.getFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
    }

    // =====================================================================
    // CREATE/UPLOAD ENDPOINTS
    // =====================================================================

    /**
     * Upload multiple documents
     * POST /api/accommodation-documents/upload
     *
     * Content-Type: multipart/form-data
     *
     * Request body should contain a "documents" array where each item has:
     * - accommodationId: The accommodation ID (obfuscated)
     * - document: The document file
     * - title: Document title (required)
     * - documentType: Document type (required)
     * - description: Optional description
     * - version: Optional version string
     * - notes: Optional internal notes
     * - validFrom: Optional validity start date
     * - validTo: Optional validity end date
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadAccommodationDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/accommodation-documents/{id}
     *
     * Updates metadata fields only (title, documentType, description, version, notes, validFrom, validTo, isActive).
     * To replace the actual document file, delete and upload a new one.
     *
     * @param id The obfuscated document ID
     * @param updateDTO The update DTO containing fields to update
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateAccommodationDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/accommodation-documents
     *
     * Permanently deletes documents from database and filesystem.
     *
     * @param ids List of obfuscated document IDs to delete
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.bulkDeleteDocuments(ids);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationDocumentRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection. Only the flags present in
     * the body apply, and per-id outcomes come back rather than a bare 200.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_DOCUMENT')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("accommodation document", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
