package com.itineraryledger.kabengosafaris.Vendor.Controller;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Vendor.DTOs.CreateVendorDTO;
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
    public ResponseEntity<ApiResponse<?>> getVendorById(@PathVariable String id) {
        log.info("GET /api/vendors/{}", id);
        return vendorGetService.getVendorById(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_VENDOR')")
    public ResponseEntity<ApiResponse<?>> getAllVendors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) VendorType type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        return vendorGetService.getAllVendors(
            name, code, type, city, country, isActive, keyword,
            page, size, sortBy, sortDirection);
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
}
