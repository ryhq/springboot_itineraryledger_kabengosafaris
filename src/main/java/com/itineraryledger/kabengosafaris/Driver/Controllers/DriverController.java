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
    public ResponseEntity<?> getDriverById(@PathVariable String idObfuscated) {
        return driverGetService.getDriverById(idObfuscated);
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
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return driverGetService.getAllDrivers(
            firstName, lastName, phone, status, isActive,
            licenseExpired, talaExpired, tourGuideIdExpired,
            keyword, page, size, sortBy, sortDirection
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
}
