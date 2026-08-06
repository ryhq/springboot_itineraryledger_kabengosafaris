package com.itineraryledger.kabengosafaris.Driver.Controllers;

import com.itineraryledger.kabengosafaris.Driver.DTOs.CreateDriverDTO;
import com.itineraryledger.kabengosafaris.Driver.DTOs.UpdateDriverDTO;
import com.itineraryledger.kabengosafaris.Driver.Enums.DriverStatus;
import com.itineraryledger.kabengosafaris.Driver.Services.CreateDriverService;
import com.itineraryledger.kabengosafaris.Driver.Services.DeleteDriverService;
import com.itineraryledger.kabengosafaris.Driver.Services.DriverGetService;
import com.itineraryledger.kabengosafaris.Driver.Services.UpdateDriverService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Slf4j
@RequiredArgsConstructor
public class DriverController {

    private final DriverGetService driverGetService;
    private final CreateDriverService createDriverService;
    private final UpdateDriverService updateDriverService;
    private final DeleteDriverService deleteDriverService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_DRIVER')")
    public ResponseEntity<?> createDriver(@Valid @RequestBody CreateDriverDTO createDTO) {
        return createDriverService.createDriver(createDTO);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_DRIVER')")
    public ResponseEntity<?> getDriverById(
        @PathVariable String idObfuscated,
        // the list's filters and sort, so prev/next stays inside the set on screen
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) DriverStatus status,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean licenseExpired,
        @RequestParam(required = false) Boolean talaExpired,
        @RequestParam(required = false) Boolean tourGuideIdExpired,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return driverGetService.getDriverById(
            idObfuscated, firstName, lastName, phone, status, isActive,
            licenseExpired, talaExpired, tourGuideIdExpired, keyword, sortBy, sortDirection
        );
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERM_READ_DRIVER')")
    public ResponseEntity<?> getDriversList() {
        return driverGetService.getDriversList();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_DRIVER')")
    public ResponseEntity<?> getAllDrivers(
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) DriverStatus status,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean licenseExpired,
        @RequestParam(required = false) Boolean talaExpired,
        @RequestParam(required = false) Boolean tourGuideIdExpired,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return driverGetService.getAllDrivers(
            firstName, lastName, phone, status, isActive,
            licenseExpired, talaExpired, tourGuideIdExpired,
            keyword, includeStats, page, size, sortBy, sortDirection
        );
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DRIVER')")
    public ResponseEntity<?> updateDriver(
        @PathVariable String idObfuscated,
        @RequestBody UpdateDriverDTO updateDTO
    ) {
        return updateDriverService.updateDriver(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_DRIVER')")
    public ResponseEntity<?> deleteDrivers(@RequestBody List<String> idObfuscatedList) {
        return deleteDriverService.deleteDrivers(idObfuscatedList);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Driver.Repository.DriverRepository bulkFlagsRepository;

    /** PATCH /bulk — activate or withdraw a whole selection in one request. */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_DRIVER')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("driver", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
