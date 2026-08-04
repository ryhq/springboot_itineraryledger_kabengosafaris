package com.itineraryledger.kabengosafaris.Park.Controllers.ParkImageControllers;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.ReorderParkImagesDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.UpdateParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.UploadParkImagesDTO;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices.ParkImageCreateService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices.ParkImageDeleteService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices.ParkImageGetService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices.ParkImageReorderService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices.ParkImageStorageService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices.ParkImageUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Park Image Management
 *
 * Provides endpoints to:
 * - List images with filters, pagination, and sorting
 * - Get image by ID
 * - Get image file by filename
 * - Serve image file by ID
 * - Upload multiple images
 * - Update image metadata
 * - Reorder images within a park
 * - Bulk delete images
 */
@RestController
@RequestMapping("/api/park-images")
@Validated
public class ParkImageController {

    private final ParkImageGetService getService;
    private final ParkImageCreateService createService;
    private final ParkImageUpdateService updateService;
    private final ParkImageReorderService reorderService;
    private final ParkImageDeleteService deleteService;
    private final ParkImageStorageService storageService;
    private final ParkImageRepository parkImageRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkImageController(
        ParkImageGetService getService,
        ParkImageCreateService createService,
        ParkImageUpdateService updateService,
        ParkImageReorderService reorderService,
        ParkImageDeleteService deleteService,
        ParkImageStorageService storageService,
        ParkImageRepository parkImageRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.reorderService = reorderService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.parkImageRepository = parkImageRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all images with filters, pagination, and sorting
     * GET /api/park-images
     *
     * Always sorts by createdAt, descending by default.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PARK_IMAGE')")
    public ResponseEntity<?> getAllImages(
            @RequestParam(value = "parkId", required = false) String parkId,
            @RequestParam(value = "parkName", required = false) String parkName,
            @RequestParam(value = "parkType", required = false) ParkType parkType,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "imageType", required = false) ImageType imageType,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "isWebActive", required = false) Boolean isWebActive,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "imageTypes", required = false) java.util.List<ParkImage.ImageType> imageTypes,
            @RequestParam(value = "statuses", required = false) java.util.List<String> statuses,
            @RequestParam(value = "qualities", required = false) java.util.List<String> qualities,
            @RequestParam(value = "createdAfter", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(value = "createdBefore", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdBefore,
            @RequestParam(required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        return getService.getAllImages(
            parkId,
            parkName,
            parkType,
            region,
            imageType,
            isPrimary,
            isActive,
            isWebActive,
            displayOrder,
            imageTypes,
            statuses,
            qualities,
            createdAfter,
            createdBefore,
            keyword,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Get image by ID
     * GET /api/park-images/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_IMAGE')")
    public ResponseEntity<?> getImageById(
            @PathVariable("id") String id,
            @RequestParam(required = false) String scopeParentId,
            // the list's filter context, so prev/next walks the set the user was in
            @RequestParam(required = false) java.util.List<com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType> imageTypes,
            @RequestParam(required = false) java.util.List<String> statuses,
            @RequestParam(required = false) java.util.List<String> qualities,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime createdAfter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return getService.getImageById(
            id, scopeParentId, imageTypes, statuses, qualities, createdAfter, sortBy, sortDirection
        );
    }

    /**
     * Get images by park ID
     * GET /api/park-images/park/{parkId}
     */
    @GetMapping("/park/{parkId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_IMAGE')")
    public ResponseEntity<?> getImagesByParkId(@PathVariable("parkId") String parkId) {
        return getService.getImagesByParkId(parkId);
    }

    /**
     * Get image file by filename
     * GET /api/park-images/file/{fileName}
     *
     * Returns the actual image file as a resource.
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageByFileName(@PathVariable("fileName") String fileName) {
        ParkImage image = getService.getImageByFileName(fileName);
        if (image == null || !image.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = storageService.readImageBytes(image.getFileName());
        if (imageBytes == null) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = image.getMimeType() != null
            ? image.getMimeType()
            : storageService.getMimeType(image.getFileName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(imageBytes.length);
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    /**
     * Serve actual image file by obfuscated ID
     * GET /api/park-images/{id}/file
     *
     * This endpoint serves the actual image bytes for display.
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getImageFile(@PathVariable("id") String id) {
        Long imageId;
        try {
            imageId = idObfuscator.decodeId(id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        ParkImage image = parkImageRepository.findById(imageId).orElse(null);
        if (image == null || !image.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = storageService.readImageBytes(image.getFileName());
        if (imageBytes == null) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = image.getMimeType() != null
            ? image.getMimeType()
            : storageService.getMimeType(image.getFileName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(imageBytes.length);
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    // =====================================================================
    // CREATE/UPLOAD ENDPOINTS
    // =====================================================================

    /**
     * Upload multiple images
     * POST /api/park-images/upload
     *
     * Content-Type: multipart/form-data
     *
     * Request body should contain an "images" array where each item has:
     * - parkId: The park ID (obfuscated)
     * - image: The image file
     * - imageType: Optional image type (defaults to OTHER)
     * - altText: Optional alt text
     * - caption: Optional caption
     * - description: Optional description
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_PARK_IMAGE')")
    public ResponseEntity<?> uploadImages(@ModelAttribute UploadParkImagesDTO uploadDTO) {
        return createService.uploadImages(uploadDTO.getImages());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update image metadata
     * PUT /api/park-images/{id}
     *
     * Updates metadata fields only (imageType, altText, caption, description, isPrimary, isActive).
     * Note: displayOrder is managed via the reorder endpoint.
     * To replace the actual image file, delete and upload a new one.
     *
     * @param id The obfuscated image ID
     * @param updateDTO The update DTO containing fields to update
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_IMAGE')")
    public ResponseEntity<?> updateImage(
            @PathVariable("id") String id,
            @RequestBody UpdateParkImageDTO updateDTO
    ) {
        return updateService.updateImage(id, updateDTO);
    }

    // =====================================================================
    // REORDER ENDPOINTS
    // =====================================================================

    /**
     * Reorder images within a park
     * POST /api/park-images/reorder
     *
     * Reorders the images based on the new order provided.
     * The image order list must contain ALL image IDs for the park.
     * The position in the list determines the new displayOrder (1-indexed).
     *
     * @param reorderDTO The reorder DTO containing park ID and new image order
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_IMAGE')")
    public ResponseEntity<?> reorderImages(@Valid @RequestBody ReorderParkImagesDTO reorderDTO) {
        return reorderService.reorderImages(reorderDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Bulk delete images by their IDs
     * DELETE /api/park-images
     *
     * Permanently deletes images from database and filesystem.
     *
     * @param ids List of obfuscated image IDs to delete
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK_IMAGE')")
    public ResponseEntity<?> bulkDeleteImages(@RequestParam("ids") List<String> ids) {
        return deleteService.bulkDeleteImages(ids);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Only the flags present in the body apply, so the same endpoint serves
     * activate, deactivate, publish, unpublish. Returns per-id
     * outcomes rather than a bare 200 that hides what did not change.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_IMAGE')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("park image", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
            if (request.getIsWebActive() != null) entity.setIsWebActive(request.getIsWebActive());
        });
    }
}
