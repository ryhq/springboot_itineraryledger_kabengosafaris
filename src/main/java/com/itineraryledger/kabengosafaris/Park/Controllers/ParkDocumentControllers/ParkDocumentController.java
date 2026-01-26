package com.itineraryledger.kabengosafaris.Park.Controllers.ParkDocumentControllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs.UpdateParkDocumentDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDocumentDTOs.UploadParkDocumentsDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkDocumentRepository;
import com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices.ParkDocumentCreateService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices.ParkDocumentDeleteService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices.ParkDocumentGetService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices.ParkDocumentStorageService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkDocumentServices.ParkDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Park Document Management
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
@RequestMapping("/api/park-documents")
@Validated
public class ParkDocumentController {

    private final ParkDocumentGetService getService;
    private final ParkDocumentCreateService createService;
    private final ParkDocumentUpdateService updateService;
    private final ParkDocumentDeleteService deleteService;
    private final ParkDocumentStorageService storageService;
    private final ParkDocumentRepository parkDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkDocumentController(
        ParkDocumentGetService getService,
        ParkDocumentCreateService createService,
        ParkDocumentUpdateService updateService,
        ParkDocumentDeleteService deleteService,
        ParkDocumentStorageService storageService,
        ParkDocumentRepository parkDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.parkDocumentRepository = parkDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/park-documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PARK_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "parkId", required = false) String parkId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "parkName", required = false) String parkName,
            @RequestParam(value = "parkType", required = false) ParkType parkType,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "tariffDocumentsOnly", required = false) Boolean tariffDocumentsOnly,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return getService.getAllDocuments(
            parkId,
            documentType,
            isActive,
            title,
            version,
            currentlyValid,
            parkName,
            parkType,
            region,
            tariffDocumentsOnly,
            sortBy,
            sortDirection,
            page,
            size
        );
    }

    /**
     * Get document by ID
     * GET /api/park-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(@PathVariable("id") String id) {
        return getService.getDocumentById(id);
    }

    /**
     * Get documents by park ID
     * GET /api/park-documents/park/{parkId}
     */
    @GetMapping("/park/{parkId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_DOCUMENT')")
    public ResponseEntity<?> getDocumentsByParkId(@PathVariable("parkId") String parkId) {
        return getService.getDocumentsByParkId(parkId);
    }

    /**
     * Get document file by filename
     * GET /api/park-documents/file/{fileName}
     *
     * Returns the actual document file as a resource.
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        ParkDocument document = getService.getDocumentEntityByFileName(fileName);
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
     * GET /api/park-documents/{id}/file
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

        ParkDocument document = parkDocumentRepository.findById(documentId).orElse(null);
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
     * POST /api/park-documents/upload
     *
     * Content-Type: multipart/form-data
     *
     * Request body should contain a "documents" array where each item has:
     * - parkId: The park ID (obfuscated)
     * - document: The document file
     * - title: The document title (required)
     * - documentType: Optional document type (defaults to OTHER)
     * - description: Optional description
     * - version: Optional version string
     * - validFrom: Optional validity start date
     * - validTo: Optional validity end date
     * - notes: Optional notes
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_PARK_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadParkDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/park-documents/{id}
     *
     * Updates metadata fields only (title, documentType, description, version, notes, validFrom, validTo, isActive).
     * To replace the actual document file, delete and upload a new one.
     *
     * @param id The obfuscated document ID
     * @param updateDTO The update DTO containing fields to update
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateParkDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Delete a single document by ID
     * DELETE /api/park-documents/{id}
     *
     * Permanently deletes the document from database and filesystem.
     *
     * @param id The obfuscated document ID to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK_DOCUMENT')")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") String id) {
        return deleteService.deleteDocument(id);
    }

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/park-documents
     *
     * Permanently deletes documents from database and filesystem.
     *
     * @param ids List of obfuscated document IDs to delete
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.deleteDocuments(ids);
    }
}
