package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.ReorderItineraryImagesDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.UpdateItineraryImageDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.DTOs.UploadItineraryImagesDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage.ImageType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository.ItineraryImageRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services.ItineraryImageCreateService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services.ItineraryImageDeleteService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services.ItineraryImageGetService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services.ItineraryImageReorderService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services.ItineraryImageStorageService;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Services.ItineraryImageUpdateService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import java.util.List;

/**
 * REST Controller for Itinerary Image Management.
 *
 * Mirrors the Park image module: list with filters/pagination, get by id,
 * serve file by filename or obfuscated id, upload, update metadata, reorder,
 * and bulk delete.
 */
@RestController
@RequestMapping("/api/itinerary-images")
@Validated
public class ItineraryImageController {

    private final ItineraryImageGetService getService;
    private final ItineraryImageCreateService createService;
    private final ItineraryImageUpdateService updateService;
    private final ItineraryImageReorderService reorderService;
    private final ItineraryImageDeleteService deleteService;
    private final ItineraryImageStorageService storageService;
    private final ItineraryImageRepository itineraryImageRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryImageController(
        ItineraryImageGetService getService,
        ItineraryImageCreateService createService,
        ItineraryImageUpdateService updateService,
        ItineraryImageReorderService reorderService,
        ItineraryImageDeleteService deleteService,
        ItineraryImageStorageService storageService,
        ItineraryImageRepository itineraryImageRepository,
        IdObfuscator idObfuscator
    ) {
        this.getService = getService;
        this.createService = createService;
        this.updateService = updateService;
        this.reorderService = reorderService;
        this.deleteService = deleteService;
        this.storageService = storageService;
        this.itineraryImageRepository = itineraryImageRepository;
        this.idObfuscator = idObfuscator;
    }

    // ===================== READ =====================

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_IMAGE')")
    public ResponseEntity<?> getAllImages(
            @RequestParam(value = "itineraryId", required = false) String itineraryId,
            @RequestParam(value = "itineraryName", required = false) String itineraryName,
            @RequestParam(value = "imageType", required = false) ImageType imageType,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "isWebActive", required = false) Boolean isWebActive,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        return getService.getAllImages(itineraryId, itineraryName, imageType, isPrimary, isActive,
            isWebActive, displayOrder, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_IMAGE')")
    public ResponseEntity<?> getImageById(
            @PathVariable("id") String id,
            @RequestParam(required = false) String scopeParentId
    ) {
        return getService.getImageById(id, scopeParentId);
    }

    @GetMapping("/itinerary/{itineraryId}")
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY_IMAGE')")
    public ResponseEntity<?> getImagesByItineraryId(@PathVariable("itineraryId") String itineraryId) {
        return getService.getImagesByItineraryId(itineraryId);
    }

    /** Public: serve image file by filename. */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageByFileName(@PathVariable("fileName") String fileName) {
        ItineraryImage image = getService.getImageByFileName(fileName);
        if (image == null || !image.getIsActive()) {
            return ResponseEntity.notFound().build();
        }
        byte[] imageBytes = storageService.readImageBytes(image.getFileName());
        if (imageBytes == null) return ResponseEntity.notFound().build();

        String mimeType = image.getMimeType() != null ? image.getMimeType() : storageService.getMimeType(image.getFileName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(imageBytes.length);
        headers.setCacheControl("public, max-age=86400");
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    /** Public: serve image file by obfuscated id. */
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getImageFile(@PathVariable("id") String id) {
        Long imageId;
        try {
            imageId = idObfuscator.decodeId(id);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        ItineraryImage image = itineraryImageRepository.findById(imageId).orElse(null);
        if (image == null || !image.getIsActive()) return ResponseEntity.notFound().build();

        byte[] imageBytes = storageService.readImageBytes(image.getFileName());
        if (imageBytes == null) return ResponseEntity.notFound().build();

        String mimeType = image.getMimeType() != null ? image.getMimeType() : storageService.getMimeType(image.getFileName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(imageBytes.length);
        headers.setCacheControl("public, max-age=86400");
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    // ===================== CREATE =====================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_ITINERARY_IMAGE')")
    public ResponseEntity<?> uploadImages(@ModelAttribute UploadItineraryImagesDTO uploadDTO) {
        return createService.uploadImages(uploadDTO.getImages());
    }

    // ===================== UPDATE =====================

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_IMAGE')")
    public ResponseEntity<?> updateImage(
            @PathVariable("id") String id,
            @RequestBody UpdateItineraryImageDTO updateDTO
    ) {
        return updateService.updateImage(id, updateDTO);
    }

    // ===================== REORDER =====================

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_IMAGE')")
    public ResponseEntity<?> reorderImages(@Valid @RequestBody ReorderItineraryImagesDTO reorderDTO) {
        return reorderService.reorderImages(reorderDTO);
    }

    // ===================== DELETE =====================

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_ITINERARY_IMAGE')")
    public ResponseEntity<?> bulkDeleteImages(@RequestParam("ids") List<String> ids) {
        return deleteService.bulkDeleteImages(ids);
    }
}
