package com.itineraryledger.kabengosafaris.Activity.Controllers.ActivityImageControllers;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.ReorderActivityImagesDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.UpdateActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.UploadActivityImagesDTO;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage.ImageType;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices.ActivityImageCreateService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices.ActivityImageDeleteService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices.ActivityImageGetService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices.ActivityImageReorderService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices.ActivityImageStorageService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices.ActivityImageUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Activity Image Management
 *
 * Provides endpoints to:
 * - List images with filters, pagination, and sorting
 * - Get image by ID
 * - Get image file by filename
 * - Serve image file by ID
 * - Upload multiple images
 * - Update image metadata
 * - Reorder images within an activity
 * - Bulk delete images
 */
@RestController
@RequestMapping("/api/activity-images")
@Validated
public class ActivityImageController {

    private final ActivityImageGetService getService;
    private final ActivityImageCreateService createService;
    private final ActivityImageUpdateService updateService;
    private final ActivityImageReorderService reorderService;
    private final ActivityImageDeleteService deleteService;
    private final ActivityImageStorageService storageService;
    private final ActivityImageRepository activityImageRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityImageController(
        ActivityImageGetService getService,
        ActivityImageCreateService createService,
        ActivityImageUpdateService updateService,
        ActivityImageReorderService reorderService,
        ActivityImageDeleteService deleteService,
        ActivityImageStorageService storageService,
        ActivityImageRepository activityImageRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.reorderService = reorderService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.activityImageRepository = activityImageRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all images with filters, pagination, and sorting
     * GET /api/activity-images
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getAllImages(
            @RequestParam(value = "activityId", required = false) String activityId,
            @RequestParam(value = "activityName", required = false) String activityName,
            @RequestParam(value = "activityIsActive", required = false) Boolean activityIsActive,
            @RequestParam(value = "hasTariff", required = false) Boolean hasTariff,
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
            activityId,
            activityName,
            activityIsActive,
            hasTariff,
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
     * GET /api/activity-images/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getImageById(
            @PathVariable("id") String id,
            @RequestParam(required = false) String scopeParentId
    ) {
        return getService.getImageById(id, scopeParentId);
    }

    /**
     * Get images by activity ID
     * GET /api/activity-images/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getImagesByActivityId(@PathVariable("activityId") String activityId) {
        return getService.getImagesByActivityId(activityId);
    }

    /**
     * Get image file by filename
     * GET /api/activity-images/file/{fileName}
     *
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageByFileName(@PathVariable("fileName") String fileName) {
        ActivityImage image = getService.getImageByFileName(fileName);
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
     * GET /api/activity-images/{id}/file
     *
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

        ActivityImage image = activityImageRepository.findById(imageId).orElse(null);
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
     * POST /api/activity-images/upload
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_ACTIVITY_IMAGE')")
    public ResponseEntity<?> uploadImages(@ModelAttribute UploadActivityImagesDTO uploadDTO) {
        return createService.uploadImages(uploadDTO.getImages());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update image metadata
     * PUT /api/activity-images/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY_IMAGE')")
    public ResponseEntity<?> updateImage(
            @PathVariable("id") String id,
            @RequestBody UpdateActivityImageDTO updateDTO
    ) {
        return updateService.updateImage(id, updateDTO);
    }

    // =====================================================================
    // REORDER ENDPOINTS
    // =====================================================================

    /**
     * Reorder images within an activity
     * POST /api/activity-images/reorder
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ACTIVITY_IMAGE')")
    public ResponseEntity<?> reorderImages(@Valid @RequestBody ReorderActivityImagesDTO reorderDTO) {
        return reorderService.reorderImages(reorderDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Bulk delete images by their IDs
     * DELETE /api/activity-images
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ACTIVITY_IMAGE')")
    public ResponseEntity<?> bulkDeleteImages(@RequestParam("ids") List<String> ids) {
        return deleteService.bulkDeleteImages(ids);
    }
}
