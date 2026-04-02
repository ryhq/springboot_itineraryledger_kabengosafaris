package com.itineraryledger.kabengosafaris.VehicleHire.Controllers.VehicleHireControllers;

import com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTOs.CreateVehicleHireDTO;
import com.itineraryledger.kabengosafaris.VehicleHire.DTOs.VehicleHireDTOs.UpdateVehicleHireDTO;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.HireStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Enums.PaymentStatus;
import com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices.CreateVehicleHireService;
import com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices.DeleteVehicleHireService;
import com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices.UpdateVehicleHireService;
import com.itineraryledger.kabengosafaris.VehicleHire.Services.VehicleHireServices.VehicleHireGetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vehicle-hires")
@Slf4j
@RequiredArgsConstructor
public class VehicleHireController {

    private final VehicleHireGetService vehicleHireGetService;
    private final CreateVehicleHireService createVehicleHireService;
    private final UpdateVehicleHireService updateVehicleHireService;
    private final DeleteVehicleHireService deleteVehicleHireService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_VEHICLE_HIRE')")
    public ResponseEntity<?> createVehicleHire(@Valid @RequestBody CreateVehicleHireDTO createDTO) {
        return createVehicleHireService.createVehicleHire(createDTO);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE_HIRE')")
    public ResponseEntity<?> getVehicleHireById(@PathVariable String idObfuscated) {
        return vehicleHireGetService.getVehicleHireById(idObfuscated);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE_HIRE')")
    public ResponseEntity<?> getAllVehicleHires(
        @RequestParam(required = false) String vehicleId,
        @RequestParam(required = false) String rentalClientId,
        @RequestParam(required = false) HireStatus status,
        @RequestParam(required = false) PaymentStatus paymentStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateAfter,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateBefore,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateAfter,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateBefore,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return vehicleHireGetService.getAllVehicleHires(
            vehicleId, rentalClientId, status, paymentStatus,
            startDateAfter, startDateBefore, endDateAfter, endDateBefore,
            keyword, page, size, sortBy, sortDirection);
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VEHICLE_HIRE')")
    public ResponseEntity<?> updateVehicleHire(@PathVariable String idObfuscated, @RequestBody UpdateVehicleHireDTO updateDTO) {
        return updateVehicleHireService.updateVehicleHire(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_VEHICLE_HIRE')")
    public ResponseEntity<?> deleteVehicleHires(@RequestBody List<String> idObfuscatedList) {
        return deleteVehicleHireService.deleteVehicleHires(idObfuscatedList);
    }
}
