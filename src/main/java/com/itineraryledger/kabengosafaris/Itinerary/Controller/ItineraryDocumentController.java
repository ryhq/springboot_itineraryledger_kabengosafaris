package com.itineraryledger.kabengosafaris.Itinerary.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs.UpdateItineraryDocumentDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs.UploadItineraryDocumentsDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.ItineraryDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryDocumentRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentGetService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentStorageService;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryDocumentServices.ItineraryDocumentUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Itinerary Document Management
 *
 * Provides endpoints to:
 * - List documents with filters, pagination, and sorting
 * - Get document by ID
 * - Get documents by itinerary ID
 * - Get document file by filename
 * - Serve document file by ID
 * - Upload multiple documents
 * - Update document metadata
 * - Delete single or bulk documents
 */
@RestController
@RequestMapping("/api/itinerary-documents")
@Validated
public class ItineraryDocumentController {

    private final ItineraryDocumentGetService getService;
    private final ItineraryDocumentCreateService createService;
    private final ItineraryDocumentUpdateService updateService;
    private final ItineraryDocumentDeleteService deleteService;
    private final ItineraryDocumentStorageService storageService;
    private final ItineraryDocumentRepository itineraryDocumentRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDocumentController(
        ItineraryDocumentGetService getService,
        ItineraryDocumentCreateService createService,
        ItineraryDocumentUpdateService updateService,
        ItineraryDocumentDeleteService deleteService,
        ItineraryDocumentStorageService storageService,
        ItineraryDocumentRepository itineraryDocumentRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.itineraryDocumentRepository = itineraryDocumentRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all documents with filters, pagination, and sorting
     * GET /api/itinerary-documents
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DOCUMENT')")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "itineraryId", required = false) String itineraryId,
            @RequestParam(value = "documentType", required = false) DocumentType documentType,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "isGenerated", required = false) Boolean isGenerated,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "currentlyValid", required = false) Boolean currentlyValid,
            @RequestParam(value = "itineraryName", required = false) String itineraryName,
            @RequestParam(value = "itineraryCode", required = false) String itineraryCode,
            @RequestParam(value = "itineraryIsActive", required = false) Boolean itineraryIsActive,
            @RequestParam(value = "itineraryStatus", required = false) ItineraryStatus itineraryStatus,
            @RequestParam(value = "tripType", required = false) TripType tripType,
            @RequestParam(value = "budgetCategory", required = false) BudgetCategory budgetCategory,
            @RequestParam(value = "quotationDocumentsOnly", required = false) Boolean quotationDocumentsOnly,
            @RequestParam(value = "travelDocumentsOnly", required = false) Boolean travelDocumentsOnly,
            @RequestParam(value = "voucherDocumentsOnly", required = false) Boolean voucherDocumentsOnly,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return getService.getAllDocuments(
            itineraryId,
            documentType,
            isActive,
            isGenerated,
            title,
            version,
            currentlyValid,
            itineraryName,
            itineraryCode,
            itineraryIsActive,
            itineraryStatus,
            tripType,
            budgetCategory,
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
     * GET /api/itinerary-documents/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DOCUMENT')")
    public ResponseEntity<?> getDocumentById(@PathVariable("id") String id) {
        return getService.getDocumentById(id);
    }

    /**
     * Get documents by itinerary ID
     * GET /api/itinerary-documents/itinerary/{itineraryId}
     */
    @GetMapping("/itinerary/{itineraryId}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_DOCUMENT')")
    public ResponseEntity<?> getDocumentsByItineraryId(@PathVariable("itineraryId") String itineraryId) {
        return getService.getDocumentsByItineraryId(itineraryId);
    }

    /**
     * Get document file by filename
     * GET /api/itinerary-documents/file/{fileName}
     *
     * No authentication required for document viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getDocumentByFileName(@PathVariable("fileName") String fileName) {
        ItineraryDocument document = getService.getDocumentEntityByFileName(fileName);
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
     * GET /api/itinerary-documents/{id}/file
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

        ItineraryDocument document = itineraryDocumentRepository.findById(documentId).orElse(null);
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
     * POST /api/itinerary-documents/upload
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_DOCUMENT')")
    public ResponseEntity<?> uploadDocuments(@ModelAttribute UploadItineraryDocumentsDTO uploadDTO) {
        return createService.uploadDocuments(uploadDTO.getDocuments());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update document metadata
     * PUT /api/itinerary-documents/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DOCUMENT')")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") String id,
            @RequestBody UpdateItineraryDocumentDTO updateDTO
    ) {
        return updateService.updateDocument(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Delete a single document by ID
     * DELETE /api/itinerary-documents/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DOCUMENT')")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") String id) {
        return deleteService.deleteDocument(id);
    }

    /**
     * Bulk delete documents by their IDs
     * DELETE /api/itinerary-documents
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_DOCUMENT')")
    public ResponseEntity<?> bulkDeleteDocuments(@RequestParam("ids") List<String> ids) {
        return deleteService.deleteDocuments(ids);
    }
}
