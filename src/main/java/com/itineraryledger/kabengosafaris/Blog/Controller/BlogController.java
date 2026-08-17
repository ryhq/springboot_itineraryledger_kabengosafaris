package com.itineraryledger.kabengosafaris.Blog.Controller;

import java.util.List;

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

import com.itineraryledger.kabengosafaris.Blog.DTOs.CreateBlogDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.UpdateBlogDTO;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogServices.BlogCreateService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogServices.BlogDeleteService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogServices.BlogGetService;
import com.itineraryledger.kabengosafaris.Blog.Services.BlogServices.BlogUpdateService;
import com.itineraryledger.kabengosafaris.Blog.Specifications.BlogFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Blog posts, for the management panel.
 *
 * The filter binds from the query string as one object, so the list, its counters and a
 * record's prev/next are all asked the same question — and adding a filter is one field
 * rather than four signatures.
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Slf4j
public class BlogController {

    private final BlogGetService getService;
    private final BlogCreateService createService;
    private final BlogUpdateService updateService;
    private final BlogDeleteService deleteService;
    private final com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;
    private final com.itineraryledger.kabengosafaris.Blog.Repository.BlogRepository blogRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_BLOG')")
    public ResponseEntity<ApiResponse<?>> getAllBlogs(
        @ModelAttribute BlogFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        return getService.getAllBlogs(filter, includeStats, page, size, sortBy, sortDirection);
    }

    /**
     * One post, plus where it sits in the caller's filtered set.
     *
     * The filter travels with it so prev/next walks the list the user was looking at rather
     * than every post in id order.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_BLOG')")
    public ResponseEntity<ApiResponse<?>> getBlogById(
        @PathVariable String id,
        @ModelAttribute BlogFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return getService.getBlogById(id, filter, sortBy, sortDirection);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_BLOG')")
    public ResponseEntity<ApiResponse<?>> createBlog(@Valid @RequestBody CreateBlogDTO createDTO) {
        return createService.createBlog(createDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BLOG')")
    public ResponseEntity<ApiResponse<?>> updateBlog(
        @PathVariable String id,
        @Valid @RequestBody UpdateBlogDTO updateDTO
    ) {
        return updateService.updateBlog(id, updateDTO);
    }

    /** Publish and unpublish, so the list can do it without sending a whole article back. */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BLOG')")
    public ResponseEntity<ApiResponse<?>> publish(@PathVariable String id) {
        return updateService.setPublished(id, true);
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BLOG')")
    public ResponseEntity<ApiResponse<?>> unpublish(@PathVariable String id) {
        return updateService.setPublished(id, false);
    }

    /**
     * PATCH /bulk — publish or unpublish a whole selection.
     *
     * Articles are written in batches and reviewed in batches; publishing six of them one
     * record at a time is the kind of work that does not get done, so they sit as drafts.
     * Only the flags present in the body apply, and it reports per-id outcomes.
     */
    @org.springframework.web.bind.annotation.PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BLOG')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("post", blogRepository, request, blog -> {
            if (request.getIsPublished() != null) blog.setIsPublished(request.getIsPublished());
        });
    }

    /** Bulk delete takes a bare array, as every other module does. */
    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_BLOG')")
    public ResponseEntity<ApiResponse<?>> deleteBlogs(@RequestBody List<String> ids) {
        return deleteService.deleteBlogs(ids);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_BLOG')")
    public ResponseEntity<ApiResponse<?>> deleteBlog(@PathVariable String id) {
        return deleteService.deleteBlogs(List.of(id));
    }
}
