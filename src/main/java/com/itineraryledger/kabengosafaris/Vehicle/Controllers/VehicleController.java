package com.itineraryledger.kabengosafaris.Vehicle.Controllers;

import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.CreateVehicleDTO;
import com.itineraryledger.kabengosafaris.Vehicle.DTOs.VehicleDTOs.UpdateVehicleDTO;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.FuelType;
import com.itineraryledger.kabengosafaris.Vehicle.Enums.VehicleType;
import com.itineraryledger.kabengosafaris.Vehicle.Services.CreateVehicleService;
import com.itineraryledger.kabengosafaris.Vehicle.Services.DeleteVehicleService;
import com.itineraryledger.kabengosafaris.Vehicle.Services.UpdateVehicleService;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleGetService;
import com.itineraryledger.kabengosafaris.Vehicle.Services.VehicleScheduleService;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Enums.SafariVehicleStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.PatchMapping;

@RestController
@RequestMapping("/api/vehicles")
@Slf4j
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleGetService vehicleGetService;
    private final CreateVehicleService createVehicleService;
    private final UpdateVehicleService updateVehicleService;
    private final DeleteVehicleService deleteVehicleService;
    private final VehicleScheduleService vehicleScheduleService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_VEHICLE')")
    public ResponseEntity<?> createVehicle(@Valid @RequestBody CreateVehicleDTO createDTO) {
        return createVehicleService.createVehicle(createDTO);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE')")
    public ResponseEntity<?> getVehicleById(
        @PathVariable String idObfuscated,
        // the list's filters and sort, so prev/next stays inside the set on screen
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String registrationNumber,
        @RequestParam(required = false) VehicleType type,
        @RequestParam(required = false) String make,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) FuelType fuelType,
        @RequestParam(required = false) Integer minCapacity,
        @RequestParam(required = false) Integer maxCapacity,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean insuranceExpired,
        @RequestParam(required = false) Boolean inspectionExpired,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return vehicleGetService.getVehicleById(
            idObfuscated, name, registrationNumber, type, make, model, year, fuelType,
            minCapacity, maxCapacity, isActive, insuranceExpired, inspectionExpired, keyword,
            sortBy, sortDirection
        );
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE')")
    public ResponseEntity<?> getVehiclesList() {
        return vehicleGetService.getVehiclesList();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE')")
    public ResponseEntity<?> getAllVehicles(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String registrationNumber,
        @RequestParam(required = false) VehicleType type,
        @RequestParam(required = false) String make,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) FuelType fuelType,
        @RequestParam(required = false) Integer minCapacity,
        @RequestParam(required = false) Integer maxCapacity,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean insuranceExpired,
        @RequestParam(required = false) Boolean inspectionExpired,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return vehicleGetService.getAllVehicles(
            name, registrationNumber, type, make, model, year, fuelType,
            minCapacity, maxCapacity, isActive, insuranceExpired, inspectionExpired,
            keyword, includeStats, page, size, sortBy, sortDirection
        );
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VEHICLE')")
    public ResponseEntity<?> updateVehicle(
        @PathVariable String idObfuscated,
        @RequestBody UpdateVehicleDTO updateDTO
    ) {
        return updateVehicleService.updateVehicle(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_VEHICLE')")
    public ResponseEntity<?> deleteVehicles(@RequestBody List<String> idObfuscatedList) {
        return deleteVehicleService.deleteVehicles(idObfuscatedList);
    }

    @GetMapping("/{vehicleId}/schedule")
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE')")
    public ResponseEntity<?> getVehicleSchedule(
        @PathVariable String vehicleId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String entryType,
        @RequestParam(required = false) SafariVehicleStatus safariStatus,
        @RequestParam(required = false) HireStatus hireStatus,
        @RequestParam(required = false) Boolean excludeCancelled
    ) {
        return vehicleScheduleService.getVehicleSchedule(vehicleId, startDate, endDate, entryType, safariStatus, hireStatus, excludeCancelled);
    }

    @GetMapping("/schedule/overview")
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE')")
    public ResponseEntity<?> getScheduleOverview(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(required = false) String entryType,
        @RequestParam(required = false) SafariVehicleStatus safariStatus,
        @RequestParam(required = false) HireStatus hireStatus,
        @RequestParam(required = false) Boolean excludeCancelled
    ) {
        return vehicleScheduleService.getScheduleOverview(startDate, endDate, entryType, safariStatus, hireStatus, excludeCancelled);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Vehicle.Repository.VehicleRepository bulkFlagsRepository;

    /** PATCH /bulk — activate or withdraw a whole selection in one request. */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VEHICLE')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("vehicle", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
