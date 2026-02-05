package com.itineraryledger.kabengosafaris.Safari.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.UpdateSafariDocumentDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs.UploadSafariDocumentsDTO;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument;
import com.itineraryledger.kabengosafaris.Safari.Entity.SafariDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariDocumentRepository;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices.SafariDocumentCreateService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices.SafariDocumentDeleteService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices.SafariDocumentGetService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices.SafariDocumentStorageService;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariDocumentServices.SafariDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Safari Document Management
 *
 * Provides endpoints to:
 * - List documents with filters, pagination, and sorting
 * - Get document by ID
 * - Get documents by safari ID
 * - Get document file by filename
 * - Serve document file by ID
 * - Upload multiple documents
 * - Update document metadata
 * - Delete single or bulk documents
 */
@RestController
@RequestMapping("/api/safari-documents")
@Validated
public class SafariDocumentController {

    private final SafariDocumentGetService getService;
    private final SafariDocumentCreateService createService;
    private final SafariDocumentUpdateService updateService;
    private final SafariDocumentDeleteService deleteService;
    private final SafariDocumentStorageService storageService;
    private final SafariDocumentRepository safariDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDocumentController(
        SafariDocumentGetService getService,
        SafariDocumentCreateService createService,
        SafariDocumentUpdateService updateService,
        SafariDocumentDeleteService deleteService,
        SafariDocumentStorageService storageService,
        SafariDocumentRepository safariDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.safariDocumentRepository = safariDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/safari-documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "safariId", required = false) String safariId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "isGenerated", required = false) Boolean isGenerated,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "safariName", required = false) String safariName,
            @RequestParam(value = "safariCode", required = false) String safariCode,
            @RequestParam(value = "safariIsActive", required = false) Boolean safariIsActive,
            @RequestParam(value = "safariState", required = false) SafariState safariState,
            @RequestParam(value = "quotationDocumentsOnly", required = false) Boolean quotationDocumentsOnly,
            @RequestParam(value = "travelDocumentsOnly", required = false) Boolean travelDocumentsOnly,
            @RequestParam(value = "voucherDocumentsOnly", required = false) Boolean voucherDocumentsOnly,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return getService.getAllDocuments(
            safariId,
            documentType,
            isActive,
            isGenerated,
            title,
            version,
            currentlyValid,
            safariName,
            safariCode,
            safariIsActive,
            safariState,
            quotationDocumentsOnly,
            travelDocumentsOnly,
            voucherDocumentsOnly,
            sortBy,
            sortDirection,
            page,
            size
        );
    }

    /**
     * Get document by ID
     * GET /api/safari-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(@PathVariable("id") String id) {
        return getService.getDocumentById(id);
    }

    /**
     * Get documents by safari ID
     * GET /api/safari-documents/safari/{safariId}
     */
    @GetMapping("/safari/{safariId}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DOCUMENT')")
    public ResponseEntity<?> getDocumentsBySafariId(@PathVariable("safariId") String safariId) {
        return getService.getDocumentsBySafariId(safariId);
    }

    /**
     * Get document file by filename
     * GET /api/safari-documents/file/{fileName}
     *
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        SafariDocument document = getService.getDocumentEntityByFileName(fileName);
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
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(documentBytes.length);
        headers.setContentDispositionFormData("inline", document.getOriginalFileName());
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
    }

    /**
     * Serve actual document file by obfuscated ID
     * GET /api/safari-documents/{id}/file
     *
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

        SafariDocument document = safariDocumentRepository.findById(documentId).orElse(null);
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
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(documentBytes.length);
        headers.setContentDispositionFormData("inline", document.getOriginalFileName());
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
    }

    // =====================================================================
    // CREATE/UPLOAD ENDPOINTS
    // =====================================================================

    /**
     * Upload multiple documents
     * POST /api/safari-documents/upload
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_SAFARI_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadSafariDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/safari-documents/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateSafariDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Delete a single document by ID
     * DELETE /api/safari-documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_DOCUMENT')")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") String id) {
        return deleteService.deleteDocument(id);
    }

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/safari-documents
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_SAFARI_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.deleteDocuments(ids);
    }
}
