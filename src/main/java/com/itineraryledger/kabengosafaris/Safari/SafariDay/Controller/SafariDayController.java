package com.itineraryledger.kabengosafaris.Safari.SafariDay.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.ReorderSafariDaysDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.UpdateSafariDayDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Services.SafariDayGetService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Services.SafariDayReorderService;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Services.SafariDayUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayController - REST controller for managing safari days
 */
@RestController
@RequestMapping("/api/safaris/{safariId}/days")
@Slf4j
public class SafariDayController {

    private final SafariDayGetService getService;
    private final SafariDayUpdateService updateService;
    private final SafariDayReorderService reorderService;

    @Autowired
    public SafariDayController(
        SafariDayGetService getService,
        SafariDayUpdateService updateService,
        SafariDayReorderService reorderService
    ) {
        this.getService = getService;
        this.updateService = updateService;
        this.reorderService = reorderService;
    }

    /**
     * Get all safari days
     *
     * GET /api/safaris/{safariId}/days
     *
     * Retrieves all days for a specific safari, ordered by day number.
     *
     * @param safariId The obfuscated safari ID
     * @return ResponseEntity with ApiResponse containing list of days
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY')")
    public ResponseEntity<ApiResponse<?>> getDays(
        @PathVariable String safariId
    ) {
        log.info("GET /api/safaris/{}/days - Fetching all days", safariId);
        return getService.getSafariDays(safariId);
    }

    /**
     * Get a specific safari day
     *
     * GET /api/safaris/{safariId}/days/{dayId}
     *
     * Retrieves details for a specific safari day.
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing the day
     */
    @GetMapping("/{dayId}")
    @PreAuthorize("hasAuthority('PERM_READ_SAFARI_DAY')")
    public ResponseEntity<ApiResponse<?>> getDay(
        @PathVariable String safariId,
        @PathVariable String dayId
    ) {
        log.info("GET /api/safaris/{}/days/{} - Fetching day", safariId, dayId);
        return getService.getSafariDay(safariId, dayId);
    }

    /**
     * Update a safari day
     *
     * PUT /api/safaris/{safariId}/days/{dayId}
     *
     * Updates the details of a specific safari day.
     * Note: dayNumber, dayTag, and actualDate cannot be changed via this endpoint.
     * Use the reorder endpoint to change day ordering and dates.
     *
     * @param safariId The obfuscated safari ID
     * @param dayId The obfuscated day ID
     * @param updateDTO The updated day data
     * @return ResponseEntity with ApiResponse containing the updated day
     */
    @PutMapping("/{dayId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY')")
    public ResponseEntity<ApiResponse<?>> updateDay(
        @PathVariable String safariId,
        @PathVariable String dayId,
        @Valid @RequestBody UpdateSafariDayDTO updateDTO
    ) {
        log.info("PUT /api/safaris/{}/days/{} - Updating safari day", safariId, dayId);
        return updateService.updateSafariDay(safariId, dayId, updateDTO);
    }

    /**
     * Reorder safari days
     *
     * POST /api/safaris/{safariId}/days/reorder
     *
     * This endpoint reorders safari days based on the provided order.
     * IMPORTANT: When days are reordered, their actualDate fields are automatically
     * recalculated based on the safari's startDate:
     *   actualDate = safari.startDate + (dayNumber - 1)
     *
     * @param safariId The obfuscated safari ID
     * @param reorderDTO The new order for the days
     * @return ResponseEntity with ApiResponse containing the reordered days
     */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_SAFARI_DAY')")
    public ResponseEntity<ApiResponse<?>> reorderDays(
        @PathVariable String safariId,
        @Valid @RequestBody ReorderSafariDaysDTO reorderDTO
    ) {
        log.info("POST /api/safaris/{}/days/reorder - Reordering {} days",
            safariId, reorderDTO.getDayOrder().size());
        return reorderService.reorderSafariDays(safariId, reorderDTO);
    }
}
