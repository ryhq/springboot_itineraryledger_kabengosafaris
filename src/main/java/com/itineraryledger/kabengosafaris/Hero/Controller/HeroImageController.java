package com.itineraryledger.kabengosafaris.Hero.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.UpdateHeroImageDTO;
import com.itineraryledger.kabengosafaris.Hero.DTOs.HeroImageDTOs.UploadHeroImagesDTO;
import com.itineraryledger.kabengosafaris.Hero.Entity.HeroImage;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroImageRepository;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageCreateService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageDeleteService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageGetService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageStorageService;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroImageServices.HeroImageUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * REST Controller for Hero Image Management
 *
 * Provides endpoints to:
 * - List images with filters, pagination, and sorting
 * - Get image by ID
 * - Get images for a hero
 * - Serve image file by ID or filename
 * - Upload multiple images
 * - Update image metadata
 * - Bulk delete images
 */
@RestController
@RequestMapping("/api/hero-images")
@Validated
@Slf4j
public class HeroImageController {

    private final HeroImageGetService getService;
    private final HeroImageCreateService createService;
    private final HeroImageUpdateService updateService;
    private final HeroImageDeleteService deleteService;
    private final HeroImageStorageService storageService;
    private final HeroImageRepository heroImageRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public HeroImageController(
        HeroImageGetService getService,
        HeroImageCreateService createService,
        HeroImageUpdateService updateService,
        HeroImageDeleteService deleteService,
        HeroImageStorageService storageService,
        HeroImageRepository heroImageRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.heroImageRepository = heroImageRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all images with filters, pagination, and sorting
     * GET /api/hero-images
     *
     * Always sorts by createdAt, descending by default.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_HERO_IMAGE')")
    public ResponseEntity<?> getAllImages(
            @RequestParam(value = "heroId", required = false) String heroId,
            @RequestParam(value = "heroTitle", required = false) String heroTitle,
            @RequestParam(value = "heroPage", required = false) HeroPage heroPage,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/hero-images - Fetching hero images with filters");
        return getService.getAllImages(
            heroId,
            heroTitle,
            heroPage,
            isPrimary,
            isActive,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    /**
     * Get image by ID
     * GET /api/hero-images/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_HERO_IMAGE')")
    public ResponseEntity<?> getImageById(
        @PathVariable("id") String id
    ) {
        log.info("GET /api/hero-images/{} - Fetching image by ID", id);
        return getService.getImageById(id);
    }

    /**
     * Get images by hero ID
     * GET /api/hero-images/hero/{heroId}
     */
    @GetMapping("/hero/{heroId}")
    @PreAuthorize("hasAuthority('PERM_READ_HERO_IMAGE')")
    public ResponseEntity<ApiResponse<?>> getHeroImages(
        @PathVariable String heroId
    ) {
        log.info("GET /api/hero-images/hero/{} - Fetching images for hero", heroId);
        return getService.getHeroImages(heroId);
    }

    /**
     * Get image file by filename
     * GET /api/hero-images/file/{fileName}
     *
     * Returns the actual image file as a resource.
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageByFileName(
        @PathVariable String fileName
    ) {
        log.info("GET /api/hero-images/file/{} - Serving image by filename", fileName);

        HeroImage image = getService.getImageByFileName(fileName);
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
     * GET /api/hero-images/{id}/file
     *
     * This endpoint serves the actual image bytes for display.
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getImageFile(
        @PathVariable String id
    ) {
        log.info("GET /api/hero-images/{}/file - Serving image by ID", id);

        Long imageId;
        try {
            imageId = idObfuscator.decodeId(id);
        } catch (Exception e) {
            log.warn("Invalid image ID: {}", id);
            return ResponseEntity.badRequest().build();
        }

        HeroImage image = heroImageRepository.findById(imageId).orElse(null);
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
     * POST /api/hero-images/upload
     *
     * Content-Type: multipart/form-data
     *
     * Request body should contain an "images" array where each item has:
     * - heroId: The hero ID (obfuscated)
     * - image: The image file
     * - altText: Optional alt text
     * - caption: Optional caption
     * - description: Optional description
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_HERO_IMAGE')")
    public ResponseEntity<ApiResponse<?>> uploadImages(
        @ModelAttribute UploadHeroImagesDTO uploadDTO
    ) {
        log.info("POST /api/hero-images/upload - Uploading {} images", uploadDTO.getImages().size());
        return createService.uploadImages(uploadDTO.getImages());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update image metadata
     * PUT /api/hero-images/{id}
     *
     * Updates metadata fields only (altText, caption, description, isPrimary, isActive).
     * To replace the actual image file, delete and upload a new one.
     *
     * @param id The obfuscated image ID
     * @param updateDTO The update DTO containing fields to update
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_HERO_IMAGE')")
    public ResponseEntity<ApiResponse<?>> updateImage(
            @PathVariable("id") String id,
            @RequestBody UpdateHeroImageDTO updateDTO
    ) {
        log.info("PUT /api/hero-images/{} - Updating image metadata", id);
        return updateService.updateImage(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Bulk delete images by their IDs
     * DELETE /api/hero-images
     *
     * Permanently deletes images from database and filesystem.
     *
     * @param ids List of obfuscated image IDs to delete
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_HERO_IMAGE')")
    public ResponseEntity<ApiResponse<?>> bulkDeleteImages(
        @RequestParam("ids") List<String> ids
    ) {
        log.info("DELETE /api/hero-images - Deleting {} images", ids.size());
        return deleteService.bulkDeleteImages(ids);
    }
}
