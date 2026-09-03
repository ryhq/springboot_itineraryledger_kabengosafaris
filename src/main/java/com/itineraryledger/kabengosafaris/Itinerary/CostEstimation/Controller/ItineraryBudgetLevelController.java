package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Controller;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.BudgetLevelComparisonDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.BudgetLevel;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Levels.ItineraryBudgetLevelAdoptService;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Levels.ItineraryBudgetLevelService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryFullGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * What the trip costs at High range, Medium and Lowest, and one request to adopt one.
 *
 * <p>Answering "what is this in luxury, and what is the cheapest you can do it for" used to mean
 * editing the days, coming back to Cost, recalculating, reading the number and repeating: three
 * mutations of a product other clients are quoted from, to answer one question about one client.
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/levels")
@RequiredArgsConstructor
@Slf4j
public class ItineraryBudgetLevelController {

    private final ItineraryFullGetService itineraryFullGetService;
    private final ItineraryBudgetLevelService levelService;
    private final ItineraryBudgetLevelAdoptService adoptService;

    /** The three columns. Reads the estimate; writes nothing. */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_ITINERARY')")
    public ResponseEntity<ApiResponse<?>> compare(
        @PathVariable String itineraryId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        LocalDate date = startDate != null ? startDate : LocalDate.now();
        try {
            FullItineraryDTO itinerary = fetch(itineraryId);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200,
                "Budget levels worked out", levelService.compare(itinerary, date)));
        } catch (Exception e) {
            log.error("Failed to work out budget levels for {}", itineraryId, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Could not work out the levels: " + e.getMessage(),
                    "BUDGET_LEVELS_FAILED"));
        }
    }

    /**
     * Adopt one level: every night's bed becomes that level's pick, in one transaction.
     *
     * <p>The same permission as promoting a single stay, because that is exactly what this does,
     * once per night.
     */
    @PostMapping("/{level}/adopt")
    @PreAuthorize("hasAuthority('PERM_UPDATE_ITINERARY_DAY_ACCOMMODATION')")
    public ResponseEntity<ApiResponse<?>> adopt(
        @PathVariable String itineraryId,
        @PathVariable String level,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        BudgetLevel target;
        try {
            target = BudgetLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400,
                    "Level must be HIGH, MEDIUM or LOWEST", "UNKNOWN_LEVEL"));
        }

        LocalDate date = startDate != null ? startDate : LocalDate.now();
        try {
            FullItineraryDTO itinerary = fetch(itineraryId);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND"));
            }
            /* Worked out here, from the tree as it is now, not from whatever the tab was holding. */
            BudgetLevelComparisonDTO comparison = levelService.compare(itinerary, date);
            return adoptService.adopt(itineraryId, comparison, target);
        } catch (Exception e) {
            log.error("Failed to adopt {} on itinerary {}", level, itineraryId, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Could not adopt that level: " + e.getMessage(),
                    "BUDGET_LEVEL_ADOPT_FAILED"));
        }
    }

    /** The full tree, out of a response that may hand it back wrapped. */
    private FullItineraryDTO fetch(String itineraryId) {
        ResponseEntity<ApiResponse<?>> response = itineraryFullGetService.getFullItinerary(itineraryId);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }
        Object data = response.getBody().getData();
        if (data instanceof FullItineraryDTO full) {
            return full;
        }
        if (data instanceof Map<?, ?> map && map.get("itinerary") instanceof FullItineraryDTO full) {
            return full;
        }
        return null;
    }
}
