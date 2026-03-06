package com.itineraryledger.kabengosafaris.Testimony.Controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs.UpdateTestimonyImageDTO;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs.UploadTestimonyImagesDTO;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyImageRepository;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageCreateService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageDeleteService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageGetService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageStorageService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageUpdateService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/api/testimony-images")
@Tag(name = "Testimony Image Management", description = "APIs for managing testimony images")
@Validated
@Slf4j
public class TestimonyImageController {

    private final TestimonyImageGetService getService;
    private final TestimonyImageCreateService createService;
    private final TestimonyImageUpdateService updateService;
    private final TestimonyImageDeleteService deleteService;
    private final TestimonyImageStorageService storageService;
    private final TestimonyImageRepository testimonyImageRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public TestimonyImageController(
        TestimonyImageGetService getService,
        TestimonyImageCreateService createService,
        TestimonyImageUpdateService updateService,
        TestimonyImageDeleteService deleteService,
        TestimonyImageStorageService storageService,
        TestimonyImageRepository testimonyImageRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.testimonyImageRepository = testimonyImageRepository;
        this.idObfuscator = idObfuscator;
    }

    // =====================================================================
    // READ ENDPOINTS
    // =====================================================================

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_TESTIMONY_IMAGE')")
    public ResponseEntity<?> getAllImages(
            @Parameter(description = "Filter by testimony ID (obfuscated)") @RequestParam(value = "testimonyId", required = false) String testimonyId,
            @Parameter(description = "Filter by primary status") @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @Parameter(description = "Filter by active status") @RequestParam(value = "isActive", required = false) Boolean isActive,
            @Parameter(description = "Search keyword") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(value = "sortBy", required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/testimony-images - Fetching testimony images with filters");
        return getService.getAllImages(testimonyId, isPrimary, isActive, keyword, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_TESTIMONY_IMAGE')")
    public ResponseEntity<?> getImageById(
        @Parameter(description = "Obfuscated image ID") @PathVariable("id") String id
    ) {
        log.info("GET /api/testimony-images/{} - Fetching image by ID", id);
        return getService.getImageById(id);
    }

    @GetMapping("/testimony/{testimonyId}")
    @PreAuthorize("hasAuthority('PERM_READ_TESTIMONY_IMAGE')")
    public ResponseEntity<ApiResponse<?>> getTestimonyImages(
        @Parameter(description = "Obfuscated testimony ID") @PathVariable String testimonyId
    ) {
        log.info("GET /api/testimony-images/testimony/{} - Fetching images for testimony", testimonyId);
        return getService.getTestimonyImages(testimonyId);
    }

    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageByFileName(
        @Parameter(description = "Image filename") @PathVariable String fileName
    ) {
        log.info("GET /api/testimony-images/file/{} - Serving image by filename", fileName);

        TestimonyImage image = getService.getImageByFileName(fileName);
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

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getImageFile(
        @Parameter(description = "Obfuscated image ID") @PathVariable String id
    ) {
        log.info("GET /api/testimony-images/{}/file - Serving image by ID", id);

        Long imageId;
        try {
            imageId = idObfuscator.decodeId(id);
        } catch (Exception e) {
            log.warn("Invalid image ID: {}", id);
            return ResponseEntity.badRequest().build();
        }

        TestimonyImage image = testimonyImageRepository.findById(imageId).orElse(null);
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

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_TESTIMONY_IMAGE')")
    public ResponseEntity<ApiResponse<?>> uploadImages(
        @Parameter(description = "Upload testimony images DTO") @ModelAttribute UploadTestimonyImagesDTO uploadDTO
    ) {
        log.info("POST /api/testimony-images/upload - Uploading {} images", uploadDTO.getImages().size());
        return createService.uploadImages(uploadDTO.getImages());
    }

    // =====================================================================
    // UPDATE ENDPOINTS
    // =====================================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TESTIMONY_IMAGE')")
    public ResponseEntity<ApiResponse<?>> updateImage(
            @Parameter(description = "Obfuscated image ID") @PathVariable("id") String id,
            @Parameter(description = "Update DTO") @RequestBody UpdateTestimonyImageDTO updateDTO
    ) {
        log.info("PUT /api/testimony-images/{} - Updating image metadata", id);
        return updateService.updateImage(id, updateDTO);
    }

    // =====================================================================
    // DELETE ENDPOINTS
    // =====================================================================

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_TESTIMONY_IMAGE')")
    public ResponseEntity<ApiResponse<?>> bulkDeleteImages(
        @Parameter(description = "List of obfuscated image IDs") @RequestParam("ids") List<String> ids
    ) {
        log.info("DELETE /api/testimony-images - Deleting {} images", ids.size());
        return deleteService.bulkDeleteImages(ids);
    }
}
