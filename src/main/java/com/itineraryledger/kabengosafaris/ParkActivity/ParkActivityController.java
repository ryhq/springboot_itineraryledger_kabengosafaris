package com.itineraryledger.kabengosafaris.ParkActivity;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityUpsertDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing park-activity associations
 */
@RestController
@RequestMapping("/api/park-activities")
@RequiredArgsConstructor
@Slf4j
public class ParkActivityController {

    private final ParkActivityService parkActivityService;

    /**
     * Get all activities for a specific park
     */
    @GetMapping("/parks/{parkIdObfuscated}/activities")
    @PreAuthorize("hasAuthority('PERM_READ_PARK') and hasAuthority('PERM_READ_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getActivitiesForPark(
        @PathVariable String parkIdObfuscated,
        @RequestParam(required = false) Boolean assigned,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String slug,
        @RequestParam(required = false) Boolean hasTariff,
        @RequestParam(required = false) Boolean isWebActive,
        @RequestParam(required = false) ChargingBasis chargingBasis,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/park-activities/parks/{}/activities - assigned: {}", parkIdObfuscated, assigned);
        return parkActivityService.getActivitiesForPark(
            parkIdObfuscated, assigned, name, slug, hasTariff, isWebActive, chargingBasis, isActive, keyword, page, size, sortDirection
        );
    }

    /**
     * Get all parks that offer a specific activity
     */
    @GetMapping("/activities/{activityIdObfuscated}/parks")
    @PreAuthorize("hasAuthority('PERM_READ_PARK') and hasAuthority('PERM_READ_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> getParksForActivity(
        @PathVariable String activityIdObfuscated,
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
        log.info("GET /api/park-activities/activities/{}/parks - assigned: {}", activityIdObfuscated, assigned);
        return parkActivityService.getParksForActivity(
            activityIdObfuscated, assigned, name, slug, parkType, region, district, location, parkSize, isActive, keyword, page, size, sortDirection
        );
    }

    /**
     * Bulk upsert park-activity relationships
     */
    @PostMapping("/upsert")
    @PreAuthorize("hasAuthority('PERM_UPDATE_PARK') and hasAuthority('PERM_UPDATE_ACTIVITY')")
    public ResponseEntity<ApiResponse<?>> upsertParkActivities(
        @Valid @RequestBody List<ParkActivityUpsertDTO> requests
    ) {
        log.info("POST /api/park-activities/upsert - Processing {} relationships", requests.size());
        return parkActivityService.upsertParkActivities(requests);
    }
}
