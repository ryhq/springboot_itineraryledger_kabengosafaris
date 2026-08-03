package com.itineraryledger.kabengosafaris.Invoice.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.UpdateInvoiceDocumentDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.UploadInvoiceDocumentsDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceDocumentRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices.InvoiceDocumentCreateService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices.InvoiceDocumentDeleteService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices.InvoiceDocumentGetService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices.InvoiceDocumentStorageService;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices.InvoiceDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Invoice Document Management
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
@RequestMapping("/api/invoice-documents")
@Validated
public class InvoiceDocumentController {

    private final InvoiceDocumentGetService getService;
    private final InvoiceDocumentCreateService createService;
    private final InvoiceDocumentUpdateService updateService;
    private final InvoiceDocumentDeleteService deleteService;
    private final InvoiceDocumentStorageService storageService;
    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public InvoiceDocumentController(
        InvoiceDocumentGetService getService,
        InvoiceDocumentCreateService createService,
        InvoiceDocumentUpdateService updateService,
        InvoiceDocumentDeleteService deleteService,
        InvoiceDocumentStorageService storageService,
        InvoiceDocumentRepository invoiceDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.invoiceDocumentRepository = invoiceDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/invoice-documents
     *
     * Always sorts by createdAt, descending by default.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "invoiceId", required = false) String invoiceId,
            @RequestParam(value = "invoiceCode", required = false) String invoiceCode,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "isGenerated", required = false) Boolean isGenerated,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        return getService.getAllDocuments(
            invoiceId,
            documentType,
            isActive,
            isGenerated,
            title,
            version,
            currentlyValid,
            invoiceCode,
            sortBy,
            sortDirection,
            page,
            size
        );
    }

    /**
     * Get document by ID
     * GET /api/invoice-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_INVOICE_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(@PathVariable("id") String id) {
        return getService.getDocumentById(id);
    }

    /**
     * Get document file by filename
     * GET /api/invoice-documents/file/{fileName}
     *
     * Returns the actual document file as a resource.
     * Example: GET /api/invoice-documents/file/f2e5a046548d723b_1701304567890.pdf
     *
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        // Find document by filename
        InvoiceDocument document = getService.getDocumentEntityByFileName(fileName);
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
        headers.setContentType(MediaType.parseMediaType(mimeType));
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
     * GET /api/invoice-documents/{id}/file
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
        InvoiceDocument document = invoiceDocumentRepository.findById(documentId).orElse(null);
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
        headers.setContentType(MediaType.parseMediaType(mimeType));
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
     * POST /api/invoice-documents/upload
     *
     * Content-Type: multipart/form-data
     *
     * Request body should contain a "documents" array where each item has:
     * - invoiceId: The invoice ID (obfuscated)
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
    @PreAuthorize("hasAuthority('PERM_CREATE_INVOICE_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadInvoiceDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/invoice-documents/{id}
     *
     * Updates metadata fields only (title, documentType, description, version, notes, validFrom, validTo, isActive).
     * To replace the actual document file, delete and upload a new one.
     *
     * @param id The obfuscated document ID
     * @param updateDTO The update DTO containing fields to update
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INVOICE_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateInvoiceDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/invoice-documents
     *
     * Permanently deletes documents from database and filesystem.
     *
     * @param ids List of obfuscated document IDs to delete
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_INVOICE_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.deleteDocuments(ids);
    }
}
