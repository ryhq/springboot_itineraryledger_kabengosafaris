package com.itineraryledger.kabengosafaris.ParkActivity.Controllers.ParkActivityImageControllers;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.ReorderParkActivityImagesDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.UpdateParkActivityImageDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs.UploadParkActivityImagesDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage.ImageType;
import com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityImageRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices.ParkActivityImageCreateService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices.ParkActivityImageDeleteService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices.ParkActivityImageGetService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices.ParkActivityImageReorderService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices.ParkActivityImageStorageService;
import com.itineraryledger.kabengosafaris.ParkActivity.Services.ParkActivityImageServices.ParkActivityImageUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Park Activity Image Management
 *
 * Provides endpoints to:
 * - List images with filters, pagination, and sorting
 * - Get image by ID
 * - Get images by park-activity, park, or activity
 * - Get image file by filename
 * - Serve image file by ID
 * - Upload multiple images
 * - Update image metadata
 * - Reorder images within a park-activity
 * - Bulk delete images
 */
@RestController
@RequestMapping("/api/park-activity-images")
@Validated
public class ParkActivityImageController {

    private final ParkActivityImageGetService getService;
    private final ParkActivityImageCreateService createService;
    private final ParkActivityImageUpdateService updateService;
    private final ParkActivityImageReorderService reorderService;
    private final ParkActivityImageDeleteService deleteService;
    private final ParkActivityImageStorageService storageService;
    private final ParkActivityImageRepository parkActivityImageRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ParkActivityImageController(
        ParkActivityImageGetService getService,
        ParkActivityImageCreateService createService,
        ParkActivityImageUpdateService updateService,
        ParkActivityImageReorderService reorderService,
        ParkActivityImageDeleteService deleteService,
        ParkActivityImageStorageService storageService,
        ParkActivityImageRepository parkActivityImageRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.reorderService = reorderService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.parkActivityImageRepository = parkActivityImageRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    /**
     * Get all images with filters, pagination, and sorting
     * GET /api/park-activity-images
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getAllImages(
            @RequestParam(value = "parkId", required = false) String parkId,
            @RequestParam(value = "activityId", required = false) String activityId,
            @RequestParam(value = "parkName", required = false) String parkName,
            @RequestParam(value = "activityName", required = false) String activityName,
            @RequestParam(value = "parkIsActive", required = false) Boolean parkIsActive,
            @RequestParam(value = "activityIsActive", required = false) Boolean activityIsActive,
            @RequestParam(value = "hasTariff", required = false) Boolean hasTariff,
            @RequestParam(value = "imageType", required = false) ImageType imageType,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        return getService.getAllImages(
            parkId,
            activityId,
            parkName,
            activityName,
            parkIsActive,
            activityIsActive,
            hasTariff,
            imageType,
            isPrimary,
            isActive,
            displayOrder,
            page,
            size,
            sortDirection
        );
    }

    /**
     * Get image by ID
     * GET /api/park-activity-images/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getImageById(@PathVariable("id") String id) {
        return getService.getImageById(id);
    }

    /**
     * Get images by park-activity (both parkId and activityId required)
     * GET /api/park-activity-images/park/{parkId}/activity/{activityId}
     */
    @GetMapping("/park/{parkId}/activity/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getImagesByParkActivity(
            @PathVariable("parkId") String parkId,
            @PathVariable("activityId") String activityId
    ) {
        return getService.getImagesByParkActivity(parkId, activityId);
    }

    /**
     * Get images by park ID (all activities)
     * GET /api/park-activity-images/park/{parkId}
     */
    @GetMapping("/park/{parkId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getImagesByParkId(@PathVariable("parkId") String parkId) {
        return getService.getImagesByParkId(parkId);
    }

    /**
     * Get images by activity ID (all parks)
     * GET /api/park-activity-images/activity/{activityId}
     */
    @GetMapping("/activity/{activityId}")
    @PreAuthorize("hasAuthority('PERM_READ_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> getImagesByActivityId(@PathVariable("activityId") String activityId) {
        return getService.getImagesByActivityId(activityId);
    }

    /**
     * Get image file by filename
     * GET /api/park-activity-images/file/{fileName}
     *
     * No authentication required for image viewing (public access).
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageByFileName(@PathVariable("fileName") String fileName) {
        ParkActivityImage image = getService.getImageByFileName(fileName);
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
     * GET /api/park-activity-images/{id}/file
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

        ParkActivityImage image = parkActivityImageRepository.findById(imageId).orElse(null);
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
     * POST /api/park-activity-images/upload
     *
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> uploadImages(@ModelAttribute UploadParkActivityImagesDTO uploadDTO) {
        return createService.uploadImages(uploadDTO.getImages());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    /**
     * Update image metadata
     * PUT /api/park-activity-images/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> updateImage(
            @PathVariable("id") String id,
            @RequestBody UpdateParkActivityImageDTO updateDTO
    ) {
        return updateService.updateImage(id, updateDTO);
    }

    // =====================================================================
    // REORDER ENDPOINTS
    // =====================================================================

    /**
     * Reorder images within a park-activity
     * POST /api/park-activity-images/reorder
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> reorderImages(@Valid @RequestBody ReorderParkActivityImagesDTO reorderDTO) {
        return reorderService.reorderImages(reorderDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    /**
     * Bulk delete images by their IDs
     * DELETE /api/park-activity-images
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_PARK_ACTIVITY_IMAGE')")
    public ResponseEntity<?> bulkDeleteImages(@RequestParam("ids") List<String> ids) {
        return deleteService.bulkDeleteImages(ids);
    }
}
