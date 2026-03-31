package com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Controller;

import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs.CreateSafariVehicleDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.DTOs.UpdateSafariVehicleDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services.SafariVehicleCreateService;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services.SafariVehicleDeleteService;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services.SafariVehicleGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariVehicle.Services.SafariVehicleUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safaris/{safariId}/vehicles")
@Slf4j
@RequiredArgsConstructor
public class SafariVehicleController {

    private final SafariVehicleGetService safariVehicleGetService;
    private final SafariVehicleCreateService safariVehicleCreateService;
    private final SafariVehicleUpdateService safariVehicleUpdateService;
    private final SafariVehicleDeleteService safariVehicleDeleteService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_VEHICLE')")
    public ResponseEntity<?> createSafariVehicle(
        @PathVariable String safariId,
        @Valid @RequestBody CreateSafariVehicleDTO createDTO
    ) {
        return safariVehicleCreateService.createSafariVehicle(safariId, createDTO);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE')")
    public ResponseEntity<?> getSafariVehicles(@PathVariable String safariId) {
        return safariVehicleGetService.getSafariVehicles(safariId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_VEHICLE')")
    public ResponseEntity<?> getSafariVehicleById(@PathVariable String safariId, @PathVariable String id) {
        return safariVehicleGetService.getSafariVehicleById(safariId, id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_VEHICLE')")
    public ResponseEntity<?> updateSafariVehicle(
        @PathVariable String safariId,
        @PathVariable String id,
        @RequestBody UpdateSafariVehicleDTO updateDTO
    ) {
        return safariVehicleUpdateService.updateSafariVehicle(safariId, id, updateDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_VEHICLE')")
    public ResponseEntity<?> deleteSafariVehicle(@PathVariable String safariId, @PathVariable String id) {
        return safariVehicleDeleteService.deleteSafariVehicle(safariId, id);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_VEHICLE')")
    public ResponseEntity<?> deleteSafariVehicles(@PathVariable String safariId, @RequestBody List<String> idList) {
        return safariVehicleDeleteService.deleteSafariVehicles(safariId, idList);
    }
}
