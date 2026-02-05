package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.SafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.ReorderSafariDayParksDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.ReorderSafariDayParksDTO.ParkVisitOrderItem;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SafariDayParkReorderService - Service for reordering park visits within a day
 *
 * Handles drag-and-drop reordering from the UI with comprehensive validations:
 * - Validates all park visit IDs exist and belong to the day
 * - Validates no duplicate IDs
 * - Validates all park visits are included (no missing)
 * - Validates expected sort orders if provided
 * - Updates sort orders
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkReorderService {

    private final SafariDayRepository safariDayRepository;
    private final SafariDayParkRepository parkVisitRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkReorderService(
        SafariDayRepository safariDayRepository,
        SafariDayParkRepository parkVisitRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.parkVisitRepository = parkVisitRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Reorder park visits based on the new order provided
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param reorderDTO The reorder data containing the new park visit order
     * @return ResponseEntity with ApiResponse containing the reordered park visits
     */
    @AuditLogAnnotation(action = "REORDER_SAFARI_DAY_PARKS", description = "Reordering park visits", entityType = "SafariDayPark")
    public ResponseEntity<ApiResponse<?>> reorderSafariDayParks(
        String safariIdObfuscated,
        String dayIdObfuscated,
        ReorderSafariDayParksDTO reorderDTO
    ) {
        log.info("Reordering park visits for day: {}", dayIdObfuscated);

        try {
            // ========================
            // DECODE IDs
            // ========================
            Long safariId;
            Long dayId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                log.warn("Invalid ID format: safari={}, day={}", safariIdObfuscated, dayIdObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // ========================
            // FIND SAFARI DAY
            // ========================
            SafariDay day = safariDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                log.warn("Safari day not found: {}", dayId);
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari day not found", "SAFARI_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to safari
            if (!day.getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // ========================
            // FETCH EXISTING PARK VISITS
            // ========================
            List<SafariDayPark> existingParkVisits = parkVisitRepository.findBySafariDayIdOrderBySortOrderAsc(dayId);

            if (existingParkVisits.isEmpty()) {
                log.warn("No park visits found for day: {}", dayId);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day has no park visits to reorder", "NO_PARK_VISITS_TO_REORDER")
                );
            }

            // ========================
            // VALIDATION: Check park visit order list size matches existing park visits
            // ========================
            List<ParkVisitOrderItem> parkVisitOrder = reorderDTO.getParkVisitOrder();

            if (parkVisitOrder.size() != existingParkVisits.size()) {
                log.warn("Park visit order count mismatch. Expected: {}, Received: {}", existingParkVisits.size(), parkVisitOrder.size());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park visit order list must contain exactly " + existingParkVisits.size() + " park visits. Received: " + parkVisitOrder.size(),
                        "PARK_VISIT_COUNT_MISMATCH"
                    )
                );
            }

            // ========================
            // DECODE ALL PARK VISIT IDs AND VALIDATE FORMAT
            // ========================
            Map<Long, ParkVisitOrderItem> decodedParkVisitIds = new LinkedHashMap<>();
            List<String> invalidIds = new ArrayList<>();
            List<String> duplicateIds = new ArrayList<>();

            for (ParkVisitOrderItem item : parkVisitOrder) {
                if (item.getParkVisitId() == null || item.getParkVisitId().isBlank()) {
                    invalidIds.add("null/empty");
                    continue;
                }

                try {
                    Long decodedId = idObfuscator.decodeId(item.getParkVisitId());

                    // Check for duplicates
                    if (decodedParkVisitIds.containsKey(decodedId)) {
                        duplicateIds.add(item.getParkVisitId());
                    } else {
                        decodedParkVisitIds.put(decodedId, item);
                    }
                } catch (Exception e) {
                    invalidIds.add(item.getParkVisitId());
                }
            }

            if (!invalidIds.isEmpty()) {
                log.warn("Invalid park visit ID formats: {}", invalidIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid park visit ID format(s): " + String.join(", ", invalidIds),
                        "INVALID_PARK_VISIT_ID_FORMAT"
                    )
                );
            }

            if (!duplicateIds.isEmpty()) {
                log.warn("Duplicate park visit IDs in reorder list: {}", duplicateIds);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Duplicate park visit ID(s) in reorder list: " + String.join(", ", duplicateIds),
                        "DUPLICATE_PARK_VISIT_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: All park visits belong to this day
            // ========================
            Set<Long> existingParkVisitIds = existingParkVisits.stream()
                .map(SafariDayPark::getId)
                .collect(Collectors.toSet());

            Set<Long> providedParkVisitIds = decodedParkVisitIds.keySet();

            // Check for park visits that don't belong to this day
            Set<Long> foreignParkVisits = new HashSet<>(providedParkVisitIds);
            foreignParkVisits.removeAll(existingParkVisitIds);

            if (!foreignParkVisits.isEmpty()) {
                List<String> foreignParkVisitObfuscated = foreignParkVisits.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Park visit IDs not belonging to day: {}", foreignParkVisitObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park visit ID(s) do not belong to this day: " + String.join(", ", foreignParkVisitObfuscated),
                        "PARK_VISIT_DAY_MISMATCH"
                    )
                );
            }

            // Check for missing park visits
            Set<Long> missingParkVisits = new HashSet<>(existingParkVisitIds);
            missingParkVisits.removeAll(providedParkVisitIds);

            if (!missingParkVisits.isEmpty()) {
                List<String> missingParkVisitObfuscated = missingParkVisits.stream()
                    .map(id -> idObfuscator.encodeId(id))
                    .collect(Collectors.toList());
                log.warn("Missing park visit IDs in reorder list: {}", missingParkVisitObfuscated);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Missing park visit ID(s) in reorder list: " + String.join(", ", missingParkVisitObfuscated),
                        "MISSING_PARK_VISIT_IDS"
                    )
                );
            }

            // ========================
            // VALIDATION: Expected sort orders (if provided)
            // ========================
            List<String> expectedOrderMismatches = new ArrayList<>();
            int position = 1;

            for (ParkVisitOrderItem item : parkVisitOrder) {
                if (item.getExpectedSortOrder() != null && !item.getExpectedSortOrder().equals(position)) {
                    expectedOrderMismatches.add(
                        String.format("Park visit %s: expected %d, but position is %d",
                            item.getParkVisitId(), item.getExpectedSortOrder(), position)
                    );
                }
                position++;
            }

            if (!expectedOrderMismatches.isEmpty()) {
                log.warn("Expected sort order mismatches: {}", expectedOrderMismatches);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Expected sort order mismatches: " + String.join("; ", expectedOrderMismatches),
                        "EXPECTED_ORDER_MISMATCH"
                    )
                );
            }

            // ========================
            // CREATE PARK VISIT LOOKUP MAP
            // ========================
            Map<Long, SafariDayPark> parkVisitLookup = existingParkVisits.stream()
                .collect(Collectors.toMap(SafariDayPark::getId, pv -> pv));

            // ========================
            // CHECK IF REORDER IS ACTUALLY NEEDED
            // ========================
            boolean orderChanged = false;
            int newSortOrder = 1;

            for (Long parkVisitId : decodedParkVisitIds.keySet()) {
                SafariDayPark parkVisit = parkVisitLookup.get(parkVisitId);
                if (!parkVisit.getSortOrder().equals(newSortOrder)) {
                    orderChanged = true;
                    break;
                }
                newSortOrder++;
            }

            if (!orderChanged) {
                log.info("Park visit order unchanged for day: {}", day.getDayTag());
                List<SafariDayParkDTO> resultDTOs = existingParkVisits.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

                return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Park visit order unchanged", resultDTOs)
                );
            }

            // ========================
            // PERFORM REORDER
            // ========================
            log.info("Performing reorder for {} park visits", existingParkVisits.size());

            // Pass 1: Set temporary negative sort orders to avoid unique constraint violations (if any)
            int tempOrder = -1;
            for (SafariDayPark parkVisit : existingParkVisits) {
                parkVisit.setSortOrder(tempOrder--);
            }
            parkVisitRepository.saveAll(existingParkVisits);
            parkVisitRepository.flush();

            // Pass 2: Set final sort orders based on new order
            List<SafariDayPark> reorderedParkVisits = new ArrayList<>();
            newSortOrder = 1;

            for (Long parkVisitId : decodedParkVisitIds.keySet()) {
                SafariDayPark parkVisit = parkVisitLookup.get(parkVisitId);
                parkVisit.setSortOrder(newSortOrder);
                reorderedParkVisits.add(parkVisit);
                newSortOrder++;
            }

            // Save all reordered park visits
            reorderedParkVisits = parkVisitRepository.saveAll(reorderedParkVisits);

            // ========================
            // CONVERT TO DTOs AND RETURN
            // ========================
            List<SafariDayParkDTO> resultDTOs = reorderedParkVisits.stream()
                .sorted(Comparator.comparing(SafariDayPark::getSortOrder))
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            log.info("Successfully reordered {} park visits for day: {}", reorderedParkVisits.size(), day.getDayTag());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visits reordered successfully", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error reordering park visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to reorder park visits", "SAFARI_DAY_PARKS_REORDER_FAILED")
            );
        }
    }

    /**
     * Convert SafariDayPark entity to DTO
     */
    private SafariDayParkDTO convertToDTO(SafariDayPark dayPark) {
        SafariDayParkDTO dto = new SafariDayParkDTO();
        dto.setId(idObfuscator.encodeId(dayPark.getId()));
        dto.setSafariDayId(idObfuscator.encodeId(dayPark.getSafariDay().getId()));
        dto.setParkId(idObfuscator.encodeId(dayPark.getPark().getId()));
        dto.setParkName(dayPark.getPark().getName());
        dto.setParkSlug(dayPark.getPark().getSlug());
        dto.setEntryType(dayPark.getEntryType());
        dto.setEntryTypeDisplayName(dayPark.getEntryType().getDisplayName());
        dto.setSortOrder(dayPark.getSortOrder());
        dto.setArrivalTime(dayPark.getArrivalTime());
        dto.setDepartureTime(dayPark.getDepartureTime());
        dto.setNotes(dayPark.getNotes());
        dto.setActualArrivalTime(dayPark.getActualArrivalTime());
        dto.setActualDepartureTime(dayPark.getActualDepartureTime());
        dto.setEntryReceiptNumber(dayPark.getEntryReceiptNumber());
        dto.setWildlifeSightings(dayPark.getWildlifeSightings());
        dto.setVisitNotes(dayPark.getVisitNotes());
        dto.setFeesPaid(dayPark.getFeesPaid());
        dto.setFeesPaidAt(dayPark.getFeesPaidAt());
        dto.setWeatherConditions(dayPark.getWeatherConditions());
        dto.setCreatedAt(dayPark.getCreatedAt());
        return dto;
    }
}
