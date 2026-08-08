package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository.ItineraryDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Replaces the whole fee set on a park visit in one call.
 *
 * The UI for this is a checklist of the park's tariffs, and a checklist is a
 * SET — not a sequence of adds and removes. Sending the state you want and
 * letting the server work out the difference means ticking three boxes cannot
 * half-succeed, and the client never has to track which rows already existed.
 *
 * Fees already on the visit are left alone rather than deleted and recreated,
 * so their notes and "included in price" survive an unrelated tick elsewhere.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryDayParkTariffSetService {

    private final ItineraryDayParkRepository dayParkRepository;
    private final ItineraryDayParkTariffRepository parkTariffRepository;
    private final ParkTariffRepository baseParkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "SET_ITINERARY_DAY_PARK_TARIFFS", description = "Replacing the fee set on a park visit", entityType = "ItineraryDayParkTariff")
    public ResponseEntity<ApiResponse<?>> setParkTariffs(String parkVisitIdObfuscated, List<String> tariffIdsObfuscated) {
        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_PARK_VISIT_ID"));
            }

            ItineraryDayPark parkVisit = dayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND"));
            }

            Long parkId = parkVisit.getPark().getId();

            // the set the caller wants, in order, without repeats
            Set<Long> wanted = new LinkedHashSet<>();
            List<String> unreadable = new ArrayList<>();
            for (String obfuscated : (tariffIdsObfuscated == null ? List.<String>of() : tariffIdsObfuscated)) {
                try {
                    wanted.add(idObfuscator.decodeId(obfuscated));
                } catch (Exception e) {
                    unreadable.add(obfuscated);
                }
            }
            if (!unreadable.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid tariff ID(s): " + String.join(", ", unreadable),
                        "INVALID_TARIFF_ID"));
            }

            /*
             * A tariff only applies here if the park charges it. Checking before
             * anything is written means a bad id cannot leave the set half
             * applied.
             */
            Map<Long, ParkTariff> assigned = baseParkTariffRepository.findByParkId(parkId).stream()
                .collect(Collectors.toMap(pt -> pt.getTariff().getId(), pt -> pt, (a, b) -> a));

            List<String> notCharged = wanted.stream()
                .filter(id -> !assigned.containsKey(id))
                .map(idObfuscator::encodeId)
                .toList();
            if (!notCharged.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        parkVisit.getPark().getName() + " does not charge tariff(s): " + String.join(", ", notCharged)
                            + ". Assign them to the park first.",
                        "TARIFF_NOT_ASSIGNED_TO_PARK"));
            }

            List<ItineraryDayParkTariff> existing = parkTariffRepository.findByItineraryDayParkId(parkVisitId);
            Set<Long> already = existing.stream()
                .map(row -> row.getParkTariff().getTariff().getId())
                .collect(Collectors.toSet());

            List<ItineraryDayParkTariff> toRemove = existing.stream()
                .filter(row -> !wanted.contains(row.getParkTariff().getTariff().getId()))
                .toList();

            List<ItineraryDayParkTariff> toAdd = wanted.stream()
                .filter(id -> !already.contains(id))
                .map(id -> ItineraryDayParkTariff.builder()
                    .itineraryDayPark(parkVisit)
                    .parkTariff(assigned.get(id))
                    .isIncludedInPrice(true)
                    .build())
                .toList();

            if (!toRemove.isEmpty()) parkTariffRepository.deleteAll(toRemove);
            if (!toAdd.isEmpty()) parkTariffRepository.saveAll(toAdd);

            int kept = existing.size() - toRemove.size();
            log.info("Park visit {} fees set: {} added, {} removed, {} unchanged",
                parkVisitIdObfuscated, toAdd.size(), toRemove.size(), kept);

            return ResponseEntity.ok(ApiResponse.success(200,
                describe(toAdd.size(), toRemove.size(), kept),
                Map.of(
                    "added", toAdd.size(),
                    "removed", toRemove.size(),
                    "unchanged", kept,
                    "total", wanted.size()
                )));

        } catch (Exception e) {
            log.error("Error setting park visit fees: {}", parkVisitIdObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to set the fees", "PARK_TARIFF_SET_FAILED"));
        }
    }

    /** What changed, in the words the UI can show without rewording. */
    private String describe(int added, int removed, int kept) {
        if (added == 0 && removed == 0) return "Fees unchanged";
        List<String> parts = new ArrayList<>();
        if (added > 0) parts.add(added + " added");
        if (removed > 0) parts.add(removed + " removed");
        if (kept > 0) parts.add(kept + " unchanged");
        return "Fees updated: " + String.join(", ", parts);
    }
}
