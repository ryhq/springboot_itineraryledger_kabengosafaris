package com.itineraryledger.kabengosafaris.Accommodation.Controllers.AccommodationImageControllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.UpdateAccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageCreateService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageDeleteService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageStorageService;
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
 * - Upload multiple images for an accommodation
 * - Update image metadata
 * - Bulk delete images
 */
@RestController
@RequestMapping("/api/accommodation-images")
@Validated
public class AccommodationImageController {

    @Autowired
    private AccommodationImageGetService getService;

    @Autowired
    private AccommodationImageCreateService createService;

    @Autowired
    private AccommodationImageDeleteService deleteService;

    @Autowired
    private AccommodationImageStorageService storageService;

    @Autowired
    private AccommodationImageRepository accommodationImageRepository;

    @Autowired
    private IdObfuscator idObfuscator;

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
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
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
            page,
            size,
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
     * Example: GET /api/accommodation-images/file/a1b2c3d4.jpg
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
     * Upload multiple images for an accommodation
     * POST /api/accommodation-images/accommodation/{accommodationId}/upload
     *
     * Content-Type: multipart/form-data
     *
     * @param accommodationId The accommodation ID (obfuscated)
     * @param files List of image files to upload
     * @param imageType Common image type for all uploads (optional, defaults to OTHER)
     */
    @PostMapping(value = "/accommodation/{accommodationId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> uploadImages(
            @PathVariable("accommodationId") String accommodationId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "imageType", required = false) ImageType imageType
    ) {
        return createService.uploadImages(accommodationId, files, imageType);
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update image metadata
     * PUT /api/accommodation-images/{id}
     *
     * Updates image metadata (not the actual image file).
     * Can update: imageType, altText, caption, description, isPrimary, isActive, displayOrder
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACCOMMODATION_IMAGE')")
    public ResponseEntity<?> updateImage(
            @PathVariable("id") String id,
            @RequestBody UpdateAccommodationImageDTO updateDTO
    ) {
        return createService.updateImage(id, updateDTO);
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
