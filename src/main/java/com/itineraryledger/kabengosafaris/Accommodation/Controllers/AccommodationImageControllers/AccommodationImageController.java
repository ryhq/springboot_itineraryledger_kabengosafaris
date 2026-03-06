package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationImageControllers;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.ReorderAccommodationImagesDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.UpdateAccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.UploadAccommodationImagesDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageCreateService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageDeleteService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageReorderService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageStorageService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Accommodation Image Management
 *
 * Provides endpoints to:
 * - List images with filters, pagination, and sorting
 * - Get image by ID
 * - Get image file by filename
 * - Serve image file by ID
 * - Upload multiple images
 * - Update image metadata
 * - Reorder images within an accommodation
 * - Bulk delete images
 */
@RestController
@RequestMapping("/api/accommodation-images")
@Validated
public class AccommodationImageController {

    private final AccommodationImageGetService getService;
    private final AccommodationImageCreateService createService;
    private final AccommodationImageUpdateService updateService;
    private final AccommodationImageReorderService reorderService;
    private final AccommodationImageDeleteService deleteService;
    private final AccommodationImageStorageService storageService;
    private final AccommodationImageRepository accommodationImageRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public AccommodationImageController(
        AccommodationImageGetService getService,
        AccommodationImageCreateService createService,
        AccommodationImageUpdateService updateService,
        AccommodationImageReorderService reorderService,
        AccommodationImageDeleteService deleteService,
        AccommodationImageStorageService storageService,
        AccommodationImageRepository accommodationImageRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.reorderService = reorderService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.accommodationImageRepository = accommodationImageRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all images with filters, pagination, and sorting
     * GET /api/accommodation-images
     *
     * Always sorts by createdAt, descending by default.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> getAllImages(
            @RequestParam(value = "accommodationId", required = false) String accommodationId,
            @RequestParam(value = "accommodationName", required = false) String accommodationName,
            @RequestParam(value = "accommodationType", required = false) AccommodationType accommodationType,
            @RequestParam(value = "accommodationCategory", required = false) AccommodationCategory accommodationCategory,
            @RequestParam(value = "imageType", required = false) ImageType imageType,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        return getService.getAllImages(
            accommodationId,
            accommodationName,
            accommodationType,
            accommodationCategory,
            imageType,
            isPrimary,
            isActive,
            displayOrder,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Get image by ID
     * GET /api/accommodation-images/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> getImageById(@PathVariable("id") String id) {
        return getService.getImageById(id);
    }

    /**
     * Get image file by filename
     * GET /api/accommodation-images/file/{fileName}
     *
     * Returns the actual image file as a resource.
     * Example: GET /api/accommodation-images/file/f2e5a046548d723b_1701304567890.jpg
     *
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageByFileName(@PathVariable("fileName") String fileName) {
        // Find image by filename
        AccommodationImage image = getService.getImageByFileName(fileName);
        if (image == null || !image.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        // Read image bytes from storage
        byte[] imageBytes = storageService.readImageBytes(image.getFileName());
        if (imageBytes == null) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
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
     * GET /api/accommodation-images/{id}/file
     *
     * This endpoint serves the actual image bytes for display.
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getImageFile(@PathVariable("id") String id) {
        // Decode image ID
        Long imageId;
        try {
            imageId = idObfuscator.decodeId(id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        // Find image
        AccommodationImage image = accommodationImageRepository.findById(imageId).orElse(null);
        if (image == null || !image.getIsActive()) {
            return ResponseEntity.notFound().build();
        }

        // Read image bytes from storage
        byte[] imageBytes = storageService.readImageBytes(image.getFileName());
        if (imageBytes == null) {
            return ResponseEntity.notFound().build();
        }

        // Determine content type
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
     * POST /api/accommodation-images/upload
     *
     * Content-Type: multipart/form-data
     *
     * Request body should contain an "images" array where each item has:
     * - accommodationId: The accommodation ID (obfuscated)
     * - image: The image file
     * - imageType: Optional image type (defaults to OTHER)
     * - altText: Optional alt text
     * - caption: Optional caption
     * - description: Optional description
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> uploadImages(@ModelAttribute UploadAccommodationImagesDTO uploadDTO) {
        return createService.uploadImages(uploadDTO.getImages());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update image metadata
     * PUT /api/accommodation-images/{id}
     *
     * Updates metadata fields only (imageType, altText, caption, description, isPrimary, isActive).
     * Note: displayOrder is managed via the reorder endpoint.
     * To replace the actual image file, delete and upload a new one.
     *
     * @param id The obfuscated image ID
     * @param updateDTO The update DTO containing fields to update
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> updateImage(
            @PathVariable("id") String id,
            @RequestBody UpdateAccommodationImageDTO updateDTO
    ) {
        return updateService.updateImage(id, updateDTO);
    }

    // =====================================================================
    // REORDER ENDPOINTS
    // =====================================================================

    /**
     * Reorder images within an accommodation
     * POST /api/accommodation-images/reorder
     *
     * Reorders the images based on the new order provided.
     * The image order list must contain ALL image IDs for the accommodation.
     * The position in the list determines the new displayOrder (1-indexed).
     *
     * @param reorderDTO The reorder DTO containing accommodation ID and new image order
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> reorderImages(@Valid @RequestBody ReorderAccommodationImagesDTO reorderDTO) {
        return reorderService.reorderImages(reorderDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Bulk delete images by their IDs
     * DELETE /api/accommodation-images
     *
     * Permanently deletes images from database and filesystem.
     *
     * @param ids List of obfuscated image IDs to delete
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> bulkDeleteImages(@RequestParam("ids") List<String> ids) {
        return deleteService.bulkDeleteImages(ids);
    }
}
