package com.itineraryledger.kabengosafaris.Vendor.Controller;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Vendor.DTOs.CreateVendorDTO;
import com.itineraryledger.kabengosafaris.Vendor.Specifications.VendorFilter;
import com.itineraryledger.kabengosafaris.Vendor.DTOs.UpdateVendorDTO;
import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import com.itineraryledger.kabengosafaris.Vendor.Services.VendorCreateService;
import com.itineraryledger.kabengosafaris.Vendor.Services.VendorDeleteService;
import com.itineraryledger.kabengosafaris.Vendor.Services.VendorGetService;
import com.itineraryledger.kabengosafaris.Vendor.Services.VendorUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@Slf4j
@RequiredArgsConstructor
public class VendorController {

    private final VendorGetService vendorGetService;
    private final VendorCreateService vendorCreateService;
    private final VendorUpdateService vendorUpdateService;
    private final VendorDeleteService vendorDeleteService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_VENDOR')")
    public ResponseEntity<ApiResponse<?>> createVendor(@Valid @RequestBody CreateVendorDTO dto) {
        log.info("POST /api/vendors - Creating vendor: {}", dto.getName());
        return vendorCreateService.createVendor(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_VENDOR')")
    public ResponseEntity<ApiResponse<?>> getVendorById(
            @PathVariable String id,
            // the list's filters and sort, so prev/next walks that same set
            @ModelAttribute VendorFilter filter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/vendors/{}", id);
        return vendorGetService.getVendorById(id, filter, sortBy, sortDirection);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_VENDOR')")
    public ResponseEntity<ApiResponse<?>> getAllVendors(
            @ModelAttribute VendorFilter filter,
            @RequestParam(required = false) Boolean includeStats,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        return vendorGetService.getAllVendors(filter, includeStats, page, size, sortBy, sortDirection);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VENDOR')")
    public ResponseEntity<ApiResponse<?>> updateVendor(
            @PathVariable String id,
            @Valid @RequestBody UpdateVendorDTO dto) {
        log.info("PUT /api/vendors/{}", id);
        return vendorUpdateService.updateVendor(id, dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_VENDOR')")
    public ResponseEntity<ApiResponse<?>> deleteVendors(@RequestBody List<String> ids) {
        log.info("DELETE /api/vendors - Deleting {} vendor(s)", ids != null ? ids.size() : 0);
        return vendorDeleteService.deleteVendors(ids);
    }

    /*
     * Retiring a vendor rather than deleting one.
     *
     * A vendor with bills against it cannot be deleted and should not be: the
     * expenses are the record of what we paid them. Deactivating keeps the
     * history and takes them out of the pickers.
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VENDOR')")
    public ResponseEntity<ApiResponse<?>> deactivate(@PathVariable String id) {
        log.info("POST /api/vendors/{}/deactivate", id);
        return vendorUpdateService.setActive(id, false);
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VENDOR')")
    public ResponseEntity<ApiResponse<?>> reactivate(@PathVariable String id) {
        log.info("POST /api/vendors/{}/reactivate", id);
        return vendorUpdateService.setActive(id, true);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — one request for a whole selection.
     *
     * Only the flags present in the body apply, and it reports per-id outcomes
     * rather than a bare 200 that hides what did not change.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VENDOR')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("vendor", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
