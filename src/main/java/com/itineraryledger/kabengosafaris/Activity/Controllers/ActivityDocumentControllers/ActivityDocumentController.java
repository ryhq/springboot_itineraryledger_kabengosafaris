package com.itineraryledger.kabengosafaris.Activity.Controllers.ActivityDocumentControllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs.UpdateActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs.UploadActivityDocumentsDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices.ActivityDocumentCreateService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices.ActivityDocumentDeleteService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices.ActivityDocumentGetService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices.ActivityDocumentStorageService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityDocumentServices.ActivityDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Activity Document Management
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
@RequestMapping("/api/activity-documents")
@Validated
public class ActivityDocumentController {

    private final ActivityDocumentGetService getService;
    private final ActivityDocumentCreateService createService;
    private final ActivityDocumentUpdateService updateService;
    private final ActivityDocumentDeleteService deleteService;
    private final ActivityDocumentStorageService storageService;
    private final ActivityDocumentRepository activityDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityDocumentController(
        ActivityDocumentGetService getService,
        ActivityDocumentCreateService createService,
        ActivityDocumentUpdateService updateService,
        ActivityDocumentDeleteService deleteService,
        ActivityDocumentStorageService storageService,
        ActivityDocumentRepository activityDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.activityDocumentRepository = activityDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/activity-documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "activityId", required = false) String activityId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "activityName", required = false) String activityName,
            @RequestParam(value = "activityIsActive", required = false) Boolean activityIsActive,
            @RequestParam(value = "hasTariff", required = false) Boolean hasTariff,
            @RequestParam(value = "safetyDocumentsOnly", required = false) Boolean safetyDocumentsOnly,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "documentTypes", required = false) java.util.List<ActivityDocument.DocumentType> documentTypes,
            @RequestParam(value = "statuses", required = false) java.util.List<String> statuses,
            @RequestParam(value = "validity", required = false) java.util.List<String> validity,
            @RequestParam(value = "createdAfter", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(value = "createdBefore", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdBefore,
            @RequestParam(required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return getService.getAllDocuments(
            activityId,
            documentType,
            isActive,
            title,
            version,
            currentlyValid,
            activityName,
            activityIsActive,
            hasTariff,
            safetyDocumentsOnly,
            documentTypes,
            statuses,
            validity,
            createdAfter,
            createdBefore,
            sortBy,
            sortDirection,
            keyword,
            page,
            size
        );
    }

    /**
     * Get document by ID
     * GET /api/activity-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(
            @PathVariable("id") String id,
            @RequestParam(required = false) String scopeParentId
    ) {
        return getService.getDocumentById(id, scopeParentId);
    }

    /**
     * Get documents by activity ID
     * GET /api/activity-documents/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getDocumentsByActivityId(@PathVariable("activityId") String activityId) {
        return getService.getDocumentsByActivityId(activityId);
    }

    /**
     * Get document file by filename
     * GET /api/activity-documents/file/{fileName}
     *
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        ActivityDocument document = getService.getDocumentEntityByFileName(fileName);
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
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition.inline()
                .filename(document.getOriginalFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
    }

    /**
     * Serve actual document file by obfuscated ID
     * GET /api/activity-documents/{id}/file
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

        ActivityDocument document = activityDocumentRepository.findById(documentId).orElse(null);
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
     * POST /api/activity-documents/upload
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadActivityDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/activity-documents/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateActivityDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Delete a single document by ID
     * DELETE /api/activity-documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") String id) {
        return deleteService.deleteDocument(id);
    }

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/activity-documents
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.deleteDocuments(ids);
    }
}
