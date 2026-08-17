package com.itineraryledger.kabengosafaris.Blog.Controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.ReorderBlogImagesDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.UpdateBlogImageDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.UploadBlogImagesDTO;
import com.itineraryledger.kabengosafaris.Blog.Entity.BlogImage;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageCreateService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageDeleteService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageGetService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageReorderService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageStorageService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogImageServices.BlogImageUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A blog post's images: upload, order, cover, delete — and the bytes themselves.
 *
 * The two file endpoints are deliberately unauthenticated in the same way the other image
 * modules are: an image URL is embedded in a page, and a browser fetching it carries no
 * bearer token. The ids are obfuscated and the file names are content hashes, so neither can
 * be walked.
 */
@RestController
@RequestMapping("/api/blog-images")
@RequiredArgsConstructor
@Slf4j
public class BlogImageController {

    private final BlogImageGetService getService;
    private final BlogImageCreateService createService;
    private final BlogImageUpdateService updateService;
    private final BlogImageDeleteService deleteService;
    private final BlogImageReorderService reorderService;
    private final BlogImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_BLOG_IMAGE')")
    public ResponseEntity<?> getAllImages(
        @RequestParam(required = false) String blogId,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        return getService.getAllImages(blogId, isPrimary, isActive, keyword, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_BLOG_IMAGE')")
    public ResponseEntity<?> getImageById(
        @PathVariable("id") String id,
        @RequestParam(required = false) String scopeParentId,
        /* the list's filters travel with the record so its arrows stay in that set */
        @RequestParam(required = false) String blogId,
        @RequestParam(required = false) Boolean isPrimary,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return getService.getImageById(id, scopeParentId, blogId, isPrimary, isActive, keyword, sortBy, sortDirection);
    }

    /** The gallery of one post, in order — what the editor reads. */
    @GetMapping("/blog/{blogId}")
    @PreAuthorize("hasAuthority('PERM_READ_BLOG_IMAGE')")
    public ResponseEntity<ApiResponse<?>> getImagesForBlog(@PathVariable String blogId) {
        return getService.getImagesForBlog(blogId);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_CREATE_BLOG_IMAGE')")
    public ResponseEntity<ApiResponse<?>> uploadImages(@ModelAttribute UploadBlogImagesDTO uploadDTO) {
        return createService.uploadImages(uploadDTO.getImages());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BLOG_IMAGE')")
    public ResponseEntity<ApiResponse<?>> updateImage(
        @PathVariable("id") String id,
        @RequestBody UpdateBlogImageDTO updateDTO
    ) {
        return updateService.updateImage(id, updateDTO);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BLOG_IMAGE')")
    public ResponseEntity<ApiResponse<?>> reorder(@Valid @RequestBody ReorderBlogImagesDTO dto) {
        return reorderService.reorder(dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_BLOG_IMAGE')")
    public ResponseEntity<ApiResponse<?>> deleteImages(@RequestBody List<String> ids) {
        return deleteService.deleteImages(ids);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_BLOG_IMAGE')")
    public ResponseEntity<ApiResponse<?>> deleteImage(@PathVariable String id) {
        return deleteService.deleteImages(List.of(id));
    }

    /* ------------------------------- the bytes ------------------------------- */

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getImageFile(@PathVariable String id) {
        try {
            BlogImage image = getService.findEntity(idObfuscator.decodeId(id));
            if (image == null || image.getFileName() == null) return ResponseEntity.notFound().build();
            return serve(image.getFileName(), image.getMimeType());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/file/{fileName}")
    public ResponseEntity<byte[]> getImageFileByName(@PathVariable String fileName) {
        BlogImage image = getService.findEntityByFileName(fileName);
        return serve(fileName, image != null ? image.getMimeType() : null);
    }

    private ResponseEntity<byte[]> serve(String fileName, String mimeType) {
        try {
            if (!storageService.imageExists(fileName)) return ResponseEntity.notFound().build();
            byte[] bytes = storageService.readImage(fileName);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                mimeType != null ? mimeType : storageService.detectMimeType(fileName)
            ));
            headers.setContentLength(bytes.length);
            /* content-addressed names never change, so this can be cached hard */
            headers.setCacheControl("public, max-age=31536000, immutable");
            return new ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (IOException e) {
            log.error("Failed to read blog image file: {}", fileName, e);
            return ResponseEntity.notFound().build();
        }
    }
}
