package com.itineraryledger.kabengosafaris.Testimony.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.CreateTestimonyDTO;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.UpdateTestimonyDTO;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyCreateService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyDeleteService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyGetService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyUpdateService;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/testimonies")
@Tag(name = "Testimony Management", description = "APIs for managing customer testimonies and reviews")
@Slf4j
public class TestimonyController {

    private final TestimonyCreateService testimonyCreateService;
    private final TestimonyUpdateService testimonyUpdateService;
    private final TestimonyDeleteService testimonyDeleteService;
    private final TestimonyGetService testimonyGetService;

    @Autowired
    public TestimonyController(
        TestimonyCreateService testimonyCreateService,
        TestimonyUpdateService testimonyUpdateService,
        TestimonyDeleteService testimonyDeleteService,
        TestimonyGetService testimonyGetService
    ) {
        this.testimonyCreateService = testimonyCreateService;
        this.testimonyUpdateService = testimonyUpdateService;
        this.testimonyDeleteService = testimonyDeleteService;
        this.testimonyGetService = testimonyGetService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> createTestimony(
        @Valid @RequestBody CreateTestimonyDTO createDTO
    ) {
        log.info("POST /api/testimonies - Creating new testimony");
        return testimonyCreateService.createTestimony(createDTO);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> getTestimonyById(
        @Parameter(description = "Obfuscated testimony ID") @PathVariable String idObfuscated
    ) {
        log.info("GET /api/testimonies/{} - Fetching testimony by ID", idObfuscated);
        return testimonyGetService.getTestimonyById(idObfuscated);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> listTestimonies(
        @Parameter(description = "Page number (0-indexed)") @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
        @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") Integer pageSize,
        @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
        @Parameter(description = "Sort direction (asc/desc)") @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @Parameter(description = "Filter by author name") @RequestParam(required = false) String authorName,
        @Parameter(description = "Filter by source") @RequestParam(required = false) TestimonySource source,
        @Parameter(description = "Filter by exact rating") @RequestParam(required = false) Integer rating,
        @Parameter(description = "Filter by minimum rating") @RequestParam(required = false) Integer minRating,
        @Parameter(description = "Filter by maximum rating") @RequestParam(required = false) Integer maxRating,
        @Parameter(description = "Filter by approved status") @RequestParam(required = false) Boolean isApproved,
        @Parameter(description = "Filter by featured status") @RequestParam(required = false) Boolean isFeatured,
        @Parameter(description = "Filter by verified booking") @RequestParam(required = false) Boolean isVerifiedBooking,
        @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive,
        @Parameter(description = "Filter by sentiment tag") @RequestParam(required = false) String sentimentTag,
        @Parameter(description = "Filter by customer ID") @RequestParam(required = false) String customerId,
        @Parameter(description = "Filter by safari ID") @RequestParam(required = false) String safariId,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword
    ) {
        log.info("GET /api/testimonies - Listing testimonies with filters");
        return testimonyGetService.listTestimonies(
            pageNumber, pageSize, sortBy, sortDirection,
            authorName, source, rating, minRating, maxRating,
            isApproved, isFeatured, isVerifiedBooking, isActive,
            sentimentTag, customerId, safariId, keyword
        );
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<?>> getPublicTestimonies() {
        log.info("GET /api/testimonies/public - Fetching public testimonies");
        return testimonyGetService.getPublicTestimonies();
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<?>> getFeaturedTestimonies() {
        log.info("GET /api/testimonies/featured - Fetching featured testimonies");
        return testimonyGetService.getFeaturedTestimonies();
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> updateTestimony(
        @Parameter(description = "Obfuscated testimony ID") @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateTestimonyDTO updateDTO
    ) {
        log.info("PUT /api/testimonies/{} - Updating testimony", idObfuscated);
        return testimonyUpdateService.updateTestimony(idObfuscated, updateDTO);
    }

    @PutMapping("/{idObfuscated}/approve")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> approveTestimony(
        @Parameter(description = "Obfuscated testimony ID") @PathVariable String idObfuscated,
        @RequestParam(required = false, defaultValue = "true") Boolean approved
    ) {
        log.info("PUT /api/testimonies/{}/approve - Approving testimony", idObfuscated);
        return testimonyUpdateService.approveTestimony(idObfuscated, approved);
    }

    @PutMapping("/{idObfuscated}/respond")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> respondToTestimony(
        @Parameter(description = "Obfuscated testimony ID") @PathVariable String idObfuscated,
        @RequestBody Map<String, String> body
    ) {
        log.info("PUT /api/testimonies/{}/respond - Adding admin response", idObfuscated);
        return testimonyUpdateService.respondToTestimony(idObfuscated, body.get("adminResponse"));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> deleteTestimonies(
        @Parameter(description = "List of obfuscated testimony IDs") @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/testimonies - Deleting {} testimonies", idObfuscatedList.size());
        return testimonyDeleteService.deleteTestimonies(idObfuscatedList);
    }
}
