package com.itineraryledger.kabengosafaris.Customer.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ContentTypes;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs.UpdateCustomerDocumentDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs.UploadCustomerDocumentsDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerDocumentRepository;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices.CustomerDocumentCreateService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices.CustomerDocumentDeleteService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices.CustomerDocumentGetService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices.CustomerDocumentStorageService;
import com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices.CustomerDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Customer Document Management
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
@RequestMapping("/api/customer-documents")
@Validated
public class CustomerDocumentController {

    private final CustomerDocumentGetService getService;
    private final CustomerDocumentCreateService createService;
    private final CustomerDocumentUpdateService updateService;
    private final CustomerDocumentDeleteService deleteService;
    private final CustomerDocumentStorageService storageService;
    private final CustomerDocumentRepository customerDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CustomerDocumentController(
        CustomerDocumentGetService getService,
        CustomerDocumentCreateService createService,
        CustomerDocumentUpdateService updateService,
        CustomerDocumentDeleteService deleteService,
        CustomerDocumentStorageService storageService,
        CustomerDocumentRepository customerDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.customerDocumentRepository = customerDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/customer-documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "customerId", required = false) String customerId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "customerType", required = false) CustomerType customerType,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "identityDocumentsOnly", required = false) Boolean identityDocumentsOnly,
            @RequestParam(value = "travelDocumentsOnly", required = false) Boolean travelDocumentsOnly,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ,
            @RequestParam(required = false) java.util.List<com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType> documentTypes,
            @RequestParam(required = false) java.util.List<String> statuses,
            @RequestParam(required = false) java.util.List<String> validity,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdBefore,
            @RequestParam(required = false) Boolean includeStats) {
        return getService.getAllDocuments(
            customerId,
            documentType,
            isActive,
            title,
            documentNumber,
            version,
            currentlyValid,
            customerName,
            customerType,
            email,
            identityDocumentsOnly,
            travelDocumentsOnly,
            sortBy,
            sortDirection,
            documentTypes,
            statuses,
            validity,
            createdAfter,
            createdBefore,
            includeStats,
            page,
            size
        );
    }

    /**
     * Get document by ID
     * GET /api/customer-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(
            @PathVariable("id") String id,
            @RequestParam(value = "scopeParentId", required = false) String scopeParentId,
            // the list's filter context, so prev/next walks the set the user was in
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.util.List<com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType> documentTypes,
            @RequestParam(required = false) java.util.List<String> statuses,
            @RequestParam(required = false) java.util.List<String> validity,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return getService.getDocumentById(
            id, scopeParentId, keyword, documentTypes, statuses, validity,
            createdAfter, sortBy, sortDirection
        );
    }

    /**
     * Get documents by customer ID with filters, pagination, and sorting
     * GET /api/customer-documents/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('PERM_READ_CUSTOMER_DOCUMENT')")
    public ResponseEntity<?> getDocumentsByCustomerId(
            @PathVariable("customerId") String customerId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "identityDocumentsOnly", required = false) Boolean identityDocumentsOnly,
            @RequestParam(value = "travelDocumentsOnly", required = false) Boolean travelDocumentsOnly,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return getService.getDocumentsByCustomerId(
            customerId,
            documentType,
            isActive,
            title,
            documentNumber,
            version,
            currentlyValid,
            identityDocumentsOnly,
            travelDocumentsOnly,
            sortBy,
            sortDirection,
            page,
            size
        );
    }

    /**
     * Get document file by filename
     * GET /api/customer-documents/file/{fileName}
     *
     * Returns the actual document file as a resource.
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        CustomerDocument document = getService.getDocumentEntityByFileName(fileName);
        if (document == null || !document.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        byte[] documentBytes = storageService.readDocumentBytes(document.getFileName());
        if (documentBytes == null) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = document.getFileType() != null
            ? document.getFileType()
            : storageService.getMimeType(document.getFileName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ContentTypes.safe(mimeType));
        headers.setContentLength(documentBytes.length);
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition.inline()
                .filename(document.getOriginalFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
    }

    /**
     * Serve actual document file by obfuscated ID
     * GET /api/customer-documents/{id}/file
     *
     * This endpoint serves the actual document bytes for display/download.
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getDocumentFile(@PathVariable("id") String id) {
        Long documentId;
        try {
            documentId = idObfuscator.decodeId(id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        CustomerDocument document = customerDocumentRepository.findById(documentId).orElse(null);
        if (document == null || !document.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        byte[] documentBytes = storageService.readDocumentBytes(document.getFileName());
        if (documentBytes == null) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = document.getFileType() != null
            ? document.getFileType()
            : storageService.getMimeType(document.getFileName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(ContentTypes.safe(mimeType));
        headers.setContentLength(documentBytes.length);
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition.inline()
                .filename(document.getOriginalFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
    }

    // =====================================================================
    // CREATE/UPLOAD ENDPOINTS
    // =====================================================================

    /**
     * Upload multiple documents
     * POST /api/customer-documents/upload
     *
     * Content-Type: multipart/form-data
     *
     * Request body should contain a "documents" array where each item has:
     * - customerId: The customer ID (obfuscated)
     * - document: The document file
     * - title: The document title (required)
     * - documentType: Optional document type (defaults to OTHER)
     * - description: Optional description
     * - documentNumber: Optional document number
     * - version: Optional version string
     * - validFrom: Optional validity start date
     * - validTo: Optional validity end date
     * - notes: Optional notes
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_CUSTOMER_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadCustomerDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/customer-documents/{id}
     *
     * Updates metadata fields only (title, documentType, description, documentNumber, version, notes, validFrom, validTo, isActive).
     * To replace the actual document file, delete and upload a new one.
     *
     * @param id The obfuscated document ID
     * @param updateDTO The update DTO containing fields to update
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateCustomerDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Delete a single document by ID
     * DELETE /api/customer-documents/{id}
     *
     * Permanently deletes the document from database and filesystem.
     *
     * @param id The obfuscated document ID to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_CUSTOMER_DOCUMENT')")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") String id) {
        return deleteService.deleteDocument(id);
    }

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/customer-documents
     *
     * Permanently deletes documents from database and filesystem.
     *
     * @param ids List of obfuscated document IDs to delete
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_CUSTOMER_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.deleteDocuments(ids);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Customer.Repository.CustomerDocumentRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Only the flags present in the body apply, so the same endpoint serves
     * activate, deactivate. Returns per-id
     * outcomes rather than a bare 200 that hides what did not change.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_CUSTOMER')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("customer document", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
