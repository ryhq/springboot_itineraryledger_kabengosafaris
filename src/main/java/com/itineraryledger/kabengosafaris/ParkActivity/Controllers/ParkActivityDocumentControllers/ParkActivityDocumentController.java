package com.itineraryledger.kabengosafaris.ParkActivity.Controllers.ParkActivityDocumentControllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.UpdateParkActivityDocumentDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs.UploadParkActivityDocumentsDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityDocument.DocumentType;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityDocumentRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices.ParkActivityDocumentCreateService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices.ParkActivityDocumentDeleteService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices.ParkActivityDocumentGetService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices.ParkActivityDocumentStorageService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityDocumentServices.ParkActivityDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Park Activity Document Management
 *
 * Provides endpoints to:
 * - List documents with filters, pagination, and sorting
 * - Get document by ID
 * - Get documents by park-activity, park, or activity
 * - Get document file by filename
 * - Serve document file by ID
 * - Upload multiple documents
 * - Update document metadata
 * - Bulk delete documents
 */
@RestController
@RequestMapping("/api/park-activity-documents")
@Validated
public class ParkActivityDocumentController {

    private final ParkActivityDocumentGetService getService;
    private final ParkActivityDocumentCreateService createService;
    private final ParkActivityDocumentUpdateService updateService;
    private final ParkActivityDocumentDeleteService deleteService;
    private final ParkActivityDocumentStorageService storageService;
    private final ParkActivityDocumentRepository parkActivityDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkActivityDocumentController(
        ParkActivityDocumentGetService getService,
        ParkActivityDocumentCreateService createService,
        ParkActivityDocumentUpdateService updateService,
        ParkActivityDocumentDeleteService deleteService,
        ParkActivityDocumentStorageService storageService,
        ParkActivityDocumentRepository parkActivityDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.parkActivityDocumentRepository = parkActivityDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/park-activity-documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "parkId", required = false) String parkId,
            @RequestParam(value = "activityId", required = false) String activityId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "parkName", required = false) String parkName,
            @RequestParam(value = "activityName", required = false) String activityName,
            @RequestParam(value = "parkIsActive", required = false) Boolean parkIsActive,
            @RequestParam(value = "activityIsActive", required = false) Boolean activityIsActive,
            @RequestParam(value = "hasTariff", required = false) Boolean hasTariff,
            @RequestParam(value = "documentTypes", required = false) java.util.List<DocumentType> documentTypes,
            @RequestParam(value = "statuses", required = false) java.util.List<String> statuses,
            @RequestParam(value = "validity", required = false) java.util.List<String> validity,
            @RequestParam(value = "createdAfter", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.LocalDateTime createdAfter,
            @RequestParam(value = "createdBefore", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.LocalDateTime createdBefore,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "includeStats", required = false) Boolean includeStats,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return getService.getAllDocuments(
            parkId,
            activityId,
            documentType,
            isActive,
            title,
            version,
            currentlyValid,
            parkName,
            activityName,
            parkIsActive,
            activityIsActive,
            hasTariff,
            documentTypes,
            statuses,
            validity,
            createdAfter,
            createdBefore,
            keyword,
            includeStats,
            sortBy,
            sortDirection,
            page,
            size
        );
    }

    /**
     * Get document by ID
     * GET /api/park-activity-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(
            @PathVariable("id") String id,
            // the list's filter context, so prev/next walks the SAME set
            @RequestParam(value = "parkId", required = false) String parkId,
            @RequestParam(value = "activityId", required = false) String activityId,
            @RequestParam(value = "documentTypes", required = false) java.util.List<DocumentType> documentTypes,
            @RequestParam(value = "statuses", required = false) java.util.List<String> statuses,
            @RequestParam(value = "validity", required = false) java.util.List<String> validity,
            @RequestParam(value = "createdAfter", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.LocalDateTime createdAfter,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", required = false) String sortDirection
    ) {
        return getService.getDocumentById(
            id, parkId, activityId, documentTypes, statuses, validity, createdAfter, keyword, sortBy, sortDirection
        );
    }

    /**
     * Get documents by park-activity (both parkId and activityId required)
     * GET /api/park-activity-documents/park/{parkId}/activity/{activityId}
     */
    @GetMapping("/park/{parkId}/activity/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getDocumentsByParkActivity(
            @PathVariable("parkId") String parkId,
            @PathVariable("activityId") String activityId
    ) {
        return getService.getDocumentsByParkActivity(parkId, activityId);
    }

    /**
     * Get documents by park ID (all activities)
     * GET /api/park-activity-documents/park/{parkId}
     */
    @GetMapping("/park/{parkId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getDocumentsByParkId(@PathVariable("parkId") String parkId) {
        return getService.getDocumentsByParkId(parkId);
    }

    /**
     * Get documents by activity ID (all parks)
     * GET /api/park-activity-documents/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> getDocumentsByActivityId(@PathVariable("activityId") String activityId) {
        return getService.getDocumentsByActivityId(activityId);
    }

    /**
     * Get document file by filename
     * GET /api/park-activity-documents/file/{fileName}
     *
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        ParkActivityDocument document = getService.getDocumentEntityByFileName(fileName);
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
     * GET /api/park-activity-documents/{id}/file
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

        ParkActivityDocument document = parkActivityDocumentRepository.findById(documentId).orElse(null);
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
     * POST /api/park-activity-documents/upload
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadParkActivityDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/park-activity-documents/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateParkActivityDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Delete a single document by ID
     * DELETE /api/park-activity-documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") String id) {
        return deleteService.deleteDocument(id);
    }

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/park-activity-documents
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK_ACTIVITY_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.deleteDocuments(ids);
    }
}
