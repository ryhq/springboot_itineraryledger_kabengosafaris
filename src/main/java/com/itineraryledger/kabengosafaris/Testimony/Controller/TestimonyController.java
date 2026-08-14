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
import com.itineraryledger.kabengosafaris.Testimony.Specifications.TestimonyFilter;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyCreateService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyDeleteService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyGetService;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyUpdateService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/testimonies")
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
        @PathVariable String idObfuscated,
        // the list's filter and sort, so prev/next stays inside the set on screen
        @ModelAttribute TestimonyFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/testimonies/{} - Fetching testimony by ID", idObfuscated);
        return testimonyGetService.getTestimonyById(idObfuscated, filter, sortBy, sortDirection);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> listTestimonies(
        /*
         * Every parameter the old signature took is still spelled the same on the wire —
         * @ModelAttribute binds them onto the filter — plus the multi-value forms
         * (sources, ratings, statuses, flags, qualities).
         */
        @ModelAttribute TestimonyFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer pageNumber,
        @RequestParam(required = false) Integer pageSize,
        /*
         * page/size are what every other list in this API is paged by. pageNumber/pageSize
         * stay because the v1 panel sends them; whichever arrives wins, house names first.
         */
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/testimonies - Listing testimonies with filters");
        return testimonyGetService.listTestimonies(
            filter,
            includeStats,
            page != null ? page : pageNumber,
            size != null ? size : pageSize,
            sortBy,
            sortDirection
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
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateTestimonyDTO updateDTO
    ) {
        log.info("PUT /api/testimonies/{} - Updating testimony", idObfuscated);
        return testimonyUpdateService.updateTestimony(idObfuscated, updateDTO);
    }

    @PutMapping("/{idObfuscated}/approve")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> approveTestimony(
        @PathVariable String idObfuscated,
        @RequestParam(required = false, defaultValue = "true") Boolean approved
    ) {
        log.info("PUT /api/testimonies/{}/approve - Approving testimony", idObfuscated);
        return testimonyUpdateService.approveTestimony(idObfuscated, approved);
    }

    @PutMapping("/{idObfuscated}/respond")
    @PreAuthorize("hasAuthority('PERM_UPDATE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> respondToTestimony(
        @PathVariable String idObfuscated,
        @RequestBody Map<String, String> body
    ) {
        log.info("PUT /api/testimonies/{}/respond - Adding admin response", idObfuscated);
        return testimonyUpdateService.respondToTestimony(idObfuscated, body.get("adminResponse"));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_TESTIMONY')")
    public ResponseEntity<ApiResponse<?>> deleteTestimonies(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/testimonies - Deleting {} testimonies", idObfuscatedList.size());
        return testimonyDeleteService.deleteTestimonies(idObfuscatedList);
    }
}
