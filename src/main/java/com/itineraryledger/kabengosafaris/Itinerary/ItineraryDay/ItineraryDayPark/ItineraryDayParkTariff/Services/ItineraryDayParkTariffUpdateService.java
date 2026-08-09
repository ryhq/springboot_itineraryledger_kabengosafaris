package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs.UpdateItineraryDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository.ItineraryDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Edits one fee on one park visit.
 *
 * The fee set could be chosen and cleared but never annotated: a fee that the
 * client is NOT paying for — already covered, or waived — could only be removed,
 * which loses the record that it was considered at all.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryDayParkTariffUpdateService {

    private final ItineraryDayParkTariffRepository parkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "UPDATE_ITINERARY_DAY_PARK_TARIFF", description = "Updating a park visit fee", entityType = "ItineraryDayParkTariff")
    public ResponseEntity<ApiResponse<?>> updateParkTariff(
        String parkVisitIdObfuscated,
        String tariffEntryIdObfuscated,
        UpdateItineraryDayParkTariffDTO updateDTO
    ) {
        try {
            Long parkVisitId;
            Long entryId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
                entryId = idObfuscator.decodeId(tariffEntryIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit or fee ID", "INVALID_ID"));
            }

            ItineraryDayParkTariff entry = parkTariffRepository.findById(entryId).orElse(null);
            if (entry == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Fee not found on this visit", "PARK_TARIFF_NOT_FOUND"));
            }
            if (!entry.getItineraryDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "That fee belongs to a different park visit", "PARK_VISIT_MISMATCH"));
            }

            if (updateDTO.getIsIncludedInPrice() != null) {
                entry.setIsIncludedInPrice(updateDTO.getIsIncludedInPrice());
            }
            // notes clear on an empty string, the house null-means-skip semantics
            if (updateDTO.getNotes() != null) {
                entry.setNotes(updateDTO.getNotes().isBlank() ? null : updateDTO.getNotes());
            }

            entry = parkTariffRepository.save(entry);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", idObfuscator.encodeId(entry.getId()));
            data.put("tariffName", entry.getParkTariff().getTariff().getName());
            data.put("isIncludedInPrice", entry.getIsIncludedInPrice());
            data.put("notes", entry.getNotes());

            return ResponseEntity.ok(ApiResponse.success(200,
                Boolean.FALSE.equals(entry.getIsIncludedInPrice())
                    ? entry.getParkTariff().getTariff().getName() + " is no longer in the quoted price"
                    : entry.getParkTariff().getTariff().getName() + " updated",
                data));

        } catch (Exception e) {
            log.error("Error updating park visit fee {}", tariffEntryIdObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update the fee", "PARK_TARIFF_UPDATE_FAILED"));
        }
    }
}
