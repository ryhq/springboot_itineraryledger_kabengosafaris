package com.itineraryledger.kabengosafaris.Safari.SafariDay.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.SafariDayDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.ReorderSafariDaysDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.DTOs.ReorderSafariDaysDTO.DayOrderItem;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SafariDayReorderService - Service for reordering safari days
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all day IDs exist and belong to the safari
 * - Validates no duplicate day IDs
 * - Validates all days are included (no missing days)
 * - Validates expected day numbers if provided
 * - Updates day numbers and regenerates day tags
 * - CRITICALLY: Recalculates actualDate for each day based on safari.startDate
 */
@Service
@Slf4j
@Transactional
public class SafariDayReorderService {

    private final SafariRepository safariRepository;
    private final SafariDayRepository safariDayRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayReorderService(
        SafariRepository safariRepository,
        SafariDayRepository safariDayRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.safariDayRepository = safariDayRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder safari days based on the new order provided
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param reorderDTO The reorder data containing the new day order
     * @return ResponseEntity with ApiResponse containing the reordered days
     */
    @AuditLogAnnotation(action = "REORDER_SAFARI_DAYS", description = "Reordering safari days", entityType = "SafariDay")
    public ResponseEntity<ApiResponse<?>> reorderSafariDays(
        String safariIdObfuscated,
        ReorderSafariDaysDTO reorderDTO
    ) {
        log.info("Reordering days for safari: {}", safariIdObfuscated);

        try {
            // ========================
            // DECODE SAFARI ID
            // ========================
            Long safariId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid safari ID format: {}", safariIdObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID format", "INVALID_SAFARI_ID")
                );
            }

            // ========================
            // FIND SAFARI
            // ========================
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                log.warn("Safari not found: {}", safariId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // ========================
            // CHECK IF SAFARI IS EDITABLE
            // ========================
            if (!safari.isEditable()) {
                log.warn("Safari is not editable: {} (state: {})", safari.getCode(), safari.getState());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Safari cannot be edited in state: " + safari.getState().getDisplayName(),
                        "SAFARI_NOT_EDITABLE"
                    )
                );
            }

            // ========================
            // FETCH EXISTING DAYS
            // ========================
            List<SafariDay> existingDays = safariDayRepository.findBySafariIdOrderByDayNumberAsc(safariId);

            if (existingDays.isEmpty()) {
                log.warn("No days found for safari: {}", safariId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Safari has no days to reorder", "NO_DAYS_TO_REORDER")
                );
            }

            // ========================
            // VALIDATION: Check day order list size matches existing days
            // ========================
            List<DayOrderItem> dayOrder = reorderDTO.getDayOrder();

            if (dayOrder.size() != existingDays.size()) {
                log.warn("Day order count mismatch. Expected: {}, Received: {}", existingDays.size(), dayOrder.size());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Day order list must contain exactly " + existingDays.size() + " days. Received: " + dayOrder.size(),
                        "DAY_COUNT_MISMATCH"
                    )
                );
            }

            // ========================
            // DECODE ALL DAY IDs AND VALIDATE FORMAT
            // ========================
            Map<Long, DayOrderItem> decodedDayIds = new LinkedHashMap<>();
            List<String> invalidIds = new ArrayList<>();
            List<String> duplicateIds = new ArrayList<>();

            for (DayOrderItem item : dayOrder) {
                if (item.getDayId() == null || item.getDayId().isBlank()) {
                    invalidIds.add("null/empty");
                    continue;
                }

                try {
                    Long decodedId = idObfuscator.decodeId(item.getDayId());

                    // Check for duplicates
                    if (decodedDayIds.containsKey(decodedId)) {
                        duplicateIds.add(item.getDayId());
                    } else {
                        decodedDayIds.put(decodedId, item);
                    }
                } catch (Exception e) {
                    invalidIds.add(item.getDayId());
                }
            }

            if (!invalidIds.isEmpty()) {
                log.warn("Invalid day ID formats: {}", invalidIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid day ID format(s): " + String.join(", ", invalidIds),
                        "INVALID_DAY_ID_FORMAT"
                    )
                );
            }

            if (!duplicateIds.isEmpty()) {
                log.warn("Duplicate day IDs in reorder list: {}", duplicateIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Duplicate day ID(s) in reorder list: " + String.join(", ", duplicateIds),
                        "DUPLICATE_DAY_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: All days belong to this safari
            // ========================
            Set<Long> existingDayIds = existingDays.stream()
                .map(SafariDay::getId)
                .collect(Collectors.toSet());

            Set<Long> providedDayIds = decodedDayIds.keySet();

            // Check for days that don't belong to this safari
            Set<Long> foreignDays = new HashSet<>(providedDayIds);
            foreignDays.removeAll(existingDayIds);

            if (!foreignDays.isEmpty()) {
                List<String> foreignDayObfuscated = foreignDays.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Day IDs not belonging to safari: {}", foreignDayObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Day ID(s) do not belong to this safari: " + String.join(", ", foreignDayObfuscated),
                        "DAY_SAFARI_MISMATCH"
                    )
                );
            }

            // Check for missing days
            Set<Long> missingDays = new HashSet<>(existingDayIds);
            missingDays.removeAll(providedDayIds);

            if (!missingDays.isEmpty()) {
                List<String> missingDayObfuscated = missingDays.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Missing day IDs in reorder list: {}", missingDayObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Missing day ID(s) in reorder list: " + String.join(", ", missingDayObfuscated),
                        "MISSING_DAY_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: Expected day numbers (if provided)
            // ========================
            List<String> expectedNumberMismatches = new ArrayList<>();
            int position = 1;

            for (DayOrderItem item : dayOrder) {
                if (item.getExpectedDayNumber() != null && !item.getExpectedDayNumber().equals(position)) {
                    expectedNumberMismatches.add(
                        String.format("Day %s: expected %d, but position is %d",
                            item.getDayId(), item.getExpectedDayNumber(), position)
                    );
                }
                position++;
            }

            if (!expectedNumberMismatches.isEmpty()) {
                log.warn("Expected day number mismatches: {}", expectedNumberMismatches);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Expected day number mismatches: " + String.join("; ", expectedNumberMismatches),
                        "EXPECTED_NUMBER_MISMATCH"
                    )
                );
            }

            // ========================
            // CREATE DAY LOOKUP MAP
            // ========================
            Map<Long, SafariDay> dayLookup = existingDays.stream()
                .collect(Collectors.toMap(SafariDay::getId, day -> day));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newDayNumber = 1;

            for (Long dayId : decodedDayIds.keySet()) {
                SafariDay day = dayLookup.get(dayId);
                if (!day.getDayNumber().equals(newDayNumber)) {
                    orderChanged = true;
                    break;
                }
                newDayNumber++;
            }

            if (!orderChanged) {
                log.info("Day order unchanged for safari: {}", safari.getCode());
                List<SafariDayDTO> resultDTOs = existingDays.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Day order unchanged", resultDTOs)
                );
            }

            // ========================
            // PERFORM REORDER
            // ========================
            // Use a two-pass approach to avoid unique constraint violations:
            // 1. First, set all day numbers to negative (temporary)
            // 2. Then, set to the final positive values and recalculate dates

            log.info("Performing reorder for {} days", existingDays.size());

            // Pass 1: Set temporary negative day numbers
            int tempNumber = -1;
            for (SafariDay day : existingDays) {
                day.setDayNumber(tempNumber--);
            }
            safariDayRepository.saveAll(existingDays);
            safariDayRepository.flush(); // Ensure changes are persisted

            // Pass 2: Set final day numbers based on new order AND recalculate dates
            List<SafariDay> reorderedDays = new ArrayList<>();
            newDayNumber = 1;
            LocalDate safariStartDate = safari.getStartDate();

            for (Long dayId : decodedDayIds.keySet()) {
                SafariDay day = dayLookup.get(dayId);
                day.setDayNumber(newDayNumber);
                day.setDayTag(null); // Clear to trigger regeneration

                // ========================
                // CRITICAL: RECALCULATE ACTUAL DATE
                // ========================
                // actualDate = safari.startDate + (dayNumber - 1)
                LocalDate newActualDate = safariStartDate.plusDays(newDayNumber - 1);
                day.setActualDate(newActualDate);

                log.debug("Day {} (ID: {}): dayNumber={}, actualDate={}",
                    day.getTitle(), dayId, newDayNumber, newActualDate);

                reorderedDays.add(day);
                newDayNumber++;
            }

            // Save all reordered days
            reorderedDays = safariDayRepository.saveAll(reorderedDays);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<SafariDayDTO> resultDTOs = reorderedDays.stream()
                .sorted(Comparator.comparing(SafariDay::getDayNumber))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} days for safari: {} (dates recalculated)",
                reorderedDays.size(), safari.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Days reordered successfully with updated dates", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering safari days", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder safari days", "SAFARI_DAYS_REORDER_FAILED")
            );
        }
    }

    /**
     * Convert SafariDay entity to SafariDayDTO
     */
    private SafariDayDTO convertToDTO(SafariDay day) {
        SafariDayDTO dto = new SafariDayDTO();
        dto.setId(idObfuscator.encodeId(day.getId()));
        dto.setSafariId(idObfuscator.encodeId(day.getSafari().getId()));
        dto.setDayNumber(day.getDayNumber());
        dto.setDayTag(day.getDayTag());
        dto.setTitle(day.getTitle());
        dto.setActualDate(day.getActualDate());
        dto.setIsPast(day.isPast());
        dto.setIsToday(day.isToday());
        dto.setIsFuture(day.isFuture());
        dto.setDescription(day.getDescription());
        dto.setMorningActivities(day.getMorningActivities());
        dto.setAfternoonActivities(day.getAfternoonActivities());
        dto.setEveningActivities(day.getEveningActivities());
        dto.setWildlifeHighlights(day.getWildlifeHighlights());
        dto.setScenicHighlights(day.getScenicHighlights());
        dto.setSpecialNotes(day.getSpecialNotes());
        dto.setStartLocation(day.getStartLocation());
        dto.setEndLocation(day.getEndLocation());
        dto.setDistanceKm(day.getDistanceKm());
        dto.setIsOvernight(day.getIsOvernight());
        dto.setMealsIncluded(day.getMealsIncluded());
        dto.setInternalNotes(day.getInternalNotes());
        dto.setWeatherNotes(day.getWeatherNotes());
        dto.setActualStartTime(day.getActualStartTime());
        dto.setActualEndTime(day.getActualEndTime());
        dto.setDriverNotes(day.getDriverNotes());
        dto.setActivitiesCount(day.getActivities() != null ? day.getActivities().size() : 0);
        dto.setParksCount(day.getParks() != null ? day.getParks().size() : 0);
        dto.setAccommodationsCount(day.getAccommodations() != null ? day.getAccommodations().size() : 0);
        dto.setCreatedAt(day.getCreatedAt());
        dto.setUpdatedAt(day.getUpdatedAt());
        return dto;
    }
}
