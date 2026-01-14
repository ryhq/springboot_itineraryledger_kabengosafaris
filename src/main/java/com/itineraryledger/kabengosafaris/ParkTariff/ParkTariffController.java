package com.itineraryledger.kabengosafaris.ParkTariff;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.ParkTariff.DTOs.ParkTariffUpsertDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing park-tariff associations
 *
 * Provides endpoints for:
 * - Getting tariffs assigned to a park
 * - Getting parks assigned to a tariff
 * - Bulk upsert (create/update/delete) park-tariff relationships
 */
@RestController
@RequestMapping("/api/park-tariffs")
@RequiredArgsConstructor
@Slf4j
public class ParkTariffController {

    private final ParkTariffService parkTariffService;

    /**
     * Get all tariffs for a specific park
     *
     * @param parkIdObfuscated Obfuscated park ID
     * @param assigned true = assigned tariffs, false = unassigned tariffs (available for assignment)
     */
    @GetMapping("/parks/{parkIdObfuscated}/tariffs")
    @PreAuthorize("hasAuthority('PERM_READ_PARK') and hasAuthority('PERM_READ_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getTariffsForPark(
        @PathVariable String parkIdObfuscated,
        @RequestParam(required = false) Boolean assigned,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String slug,
        @RequestParam(required = false) ChargingBasis chargingBasis,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isSystem,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        log.info("GET /api/park-tariffs/parks/{}/tariffs - assigned: {}", parkIdObfuscated, assigned);
        return parkTariffService.getTariffsForPark(
            parkIdObfuscated, assigned, name, slug, chargingBasis, isActive, isSystem, keyword, page, size, sortDirection
        );
    }

    /**
     * Get all parks that have a specific tariff assigned
     *
     * @param tariffIdObfuscated Obfuscated tariff ID
     * @param assigned true = assigned parks, false = unassigned parks (available for assignment)
     */
    @GetMapping("/tariffs/{tariffIdObfuscated}/parks")
    @PreAuthorize("hasAuthority('PERM_READ_PARK') and hasAuthority('PERM_READ_TARIFF')")
    public ResponseEntity<ApiResponse<?>> getParksForTariff(
        @PathVariable String tariffIdObfuscated,
        @RequestParam(required = false) Boolean assigned,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String slug,
        @RequestParam(required = false) ParkType parkType,
        @RequestParam(required = false) String region,
        @RequestParam(required = false) String district,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String parkSize,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/park-tariffs/tariffs/{}/parks - assigned: {}", tariffIdObfuscated, assigned);
        return parkTariffService.getParksForTariff(
            tariffIdObfuscated, assigned, name, slug, parkType, region, district, location, parkSize, isActive, keyword, page, size, sortDirection
        );
    }

    /**
     * Bulk upsert park-tariff relationships
     *
     * Supports creating, updating, and deleting multiple relationships in a single request.
     * - status = true: Create or update the relationship
     * - status = false: Delete the relationship
     */
    @PostMapping("/upsert")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK') and hasAuthority('PERM_UPDATE_TARIFF')")
    public ResponseEntity<ApiResponse<?>> upsertParkTariffs(
        @Valid @RequestBody List<ParkTariffUpsertDTO> requests
    ) {
        log.info("POST /api/park-tariffs/upsert - Processing {} relationships", requests.size());
        return parkTariffService.upsertParkTariffs(requests);
    }
}
