package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Levels;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.BudgetLevelComparisonDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.BudgetLevel;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Services.ItineraryDayAccommodationUpdateService;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Take one of the three levels and make it the trip.
 *
 * <p>One transaction over every night. As a request per night it can half-apply, and a half-applied
 * level is not a cheaper trip: it is a trip whose Serengeti nights are budget and whose Ngorongoro
 * nights are luxury, quoted as though somebody meant it.
 *
 * <p>The picks are recomputed here rather than taken from the caller. A browser tab open since this
 * morning is holding yesterday's beds, and adopting from it would promote rows somebody has since
 * changed. The screen proposes; the server decides what it is actually promoting.
 *
 * <p>Nothing is destroyed. The bed that was booked becomes an alternative, so adopting another
 * level -- or the one you started on -- puts it back. That is why there is no undo: there is
 * nothing to undo, only another column to adopt.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryBudgetLevelAdoptService {

    private final ItineraryBudgetLevelService levelService;
    private final ItineraryDayAccommodationUpdateService accommodationUpdateService;
    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    @Transactional
    public ResponseEntity<ApiResponse<?>> adopt(
        String itineraryIdObfuscated,
        BudgetLevelComparisonDTO comparison,
        BudgetLevel level
    ) {
        Long itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
        if (itineraryId == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid itinerary id", "INVALID_ID"));
        }

        Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
        if (itinerary == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND"));
        }

        BudgetLevelComparisonDTO.LevelDTO chosen = comparison.getLevels().stream()
            .filter(l -> l.getLevel() == level)
            .findFirst()
            .orElse(null);
        if (chosen == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No such level", "UNKNOWN_LEVEL"));
        }

        List<String> changes = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (BudgetLevelComparisonDTO.NightPickDTO night : chosen.getNights()) {
            if (Boolean.TRUE.equals(night.getIsCurrentPrimary())) {
                continue;
            }
            Long dayId = idObfuscator.decodeId(night.getDayId());
            Long entryId = idObfuscator.decodeId(night.getEntryId());
            if (dayId == null || entryId == null) {
                skipped.add(reason(night, "the day or the stay could not be identified"));
                continue;
            }

            accommodationUpdateService.promote(dayId, entryId);
            changes.add(String.format("Day %d: %s", night.getDayNumber(), night.getAccommodationName()));
        }

        /*
         * The badge follows the beds, so the header stops contradicting them. Read off what was
         * actually adopted rather than from the level's name: adopting High on a trip whose dearest
         * recorded options are mid-range does not make it a luxury itinerary, and saying so would
         * be the header lying in a new direction.
         */
        String badge = chosen.getBadge();
        String previousBadge = itinerary.getBudgetCategory() == null ? null
            : itinerary.getBudgetCategory().name();
        if (badge != null && !badge.equals(previousBadge)) {
            try {
                itinerary.setBudgetCategory(BudgetCategory.valueOf(
                    BudgetLevel.badgeFor(AccommodationCategory.valueOf(badge)).name()));
                itineraryRepository.save(itinerary);
            } catch (IllegalArgumentException e) {
                log.warn("Could not map adopted category {} onto a budget category", badge);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("level", level.name());
        report.put("label", level.getLabel());
        report.put("nightsChanged", changes.size());
        report.put("nightsAlreadyThere", chosen.getNightsAlreadyThere());
        report.put("nightsWithoutAChoice", chosen.getNightsWithoutAChoice());
        report.put("changes", changes);
        report.put("skipped", skipped);
        report.put("budgetCategory", itinerary.getBudgetCategory() == null ? null
            : itinerary.getBudgetCategory().name());

        log.info("Itinerary {} adopted {} — {} night(s) changed", itineraryId, level, changes.size());

        return ResponseEntity.ok(ApiResponse.success(200,
            changes.isEmpty()
                ? "Every night was already at this level"
                : String.format("%d night%s changed", changes.size(), changes.size() == 1 ? "" : "s"),
            report));
    }

    private Map<String, Object> reason(BudgetLevelComparisonDTO.NightPickDTO night, String why) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dayNumber", night.getDayNumber());
        row.put("accommodationName", night.getAccommodationName());
        row.put("reason", why);
        return row;
    }
}
