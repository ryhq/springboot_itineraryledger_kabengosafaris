package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.ItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.UpdateItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkUpdateService - Service for updating itinerary day park visits
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayParkUpdateService {

    private final ItineraryDayParkRepository itineraryDayParkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkUpdateService(
        ItineraryDayParkRepository itineraryDayParkRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayParkRepository = itineraryDayParkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an itinerary day park visit
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated park visit
     */
    @AuditLogAnnotation(action = "UPDATE_ITINERARY_DAY_PARK", description = "Updating park visit", entityType = "ItineraryDayPark")
    public ResponseEntity<ApiResponse<?>> updateItineraryDayPark(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        String parkVisitIdObfuscated,
        UpdateItineraryDayParkDTO updateDTO
    ) {
        log.info("Updating park visit: {}", parkVisitIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long parkVisitId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Find park visit
            ItineraryDayPark parkVisit = itineraryDayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!parkVisit.getItineraryDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park visit does not belong to this day", "PARK_VISIT_DAY_MISMATCH")
                );
            }

            if (!parkVisit.getItineraryDay().getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Update fields if provided
            if (updateDTO.getEntryType() != null) {
                parkVisit.setEntryType(updateDTO.getEntryType().isBlank() ? null : com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType.valueOf(updateDTO.getEntryType().trim()));
            }
            if (updateDTO.getArrivalTime() != null) {
                parkVisit.setArrivalTime(updateDTO.getArrivalTime());
            }
            if (updateDTO.getDepartureTime() != null) {
                parkVisit.setDepartureTime(updateDTO.getDepartureTime());
            }
            if (updateDTO.getNotes() != null) {
                parkVisit.setNotes(updateDTO.getNotes());
            }

            // Save
            parkVisit = itineraryDayParkRepository.save(parkVisit);

            // Convert to DTO
            ItineraryDayParkDTO dto = convertToDTO(parkVisit);

            log.info("Park visit updated successfully: {}", parkVisitId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visit updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating park visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update park visit", "ITINERARY_DAY_PARK_UPDATE_FAILED")
            );
        }
    }

    /**
     * Convert ItineraryDayPark entity to DTO
     */
    private ItineraryDayParkDTO convertToDTO(ItineraryDayPark dayPark) {
        ItineraryDayParkDTO dto = new ItineraryDayParkDTO();
        dto.setId(idObfuscator.encodeId(dayPark.getId()));
        dto.setItineraryDayId(idObfuscator.encodeId(dayPark.getItineraryDay().getId()));
        dto.setParkId(idObfuscator.encodeId(dayPark.getPark().getId()));
        dto.setParkName(dayPark.getPark().getName());
        dto.setParkSlug(dayPark.getPark().getSlug());
        dto.setEntryType(dayPark.getEntryType());
        dto.setEntryTypeDisplayName(dayPark.getEntryType().getDisplayName());
        dto.setSortOrder(dayPark.getSortOrder());
        dto.setArrivalTime(dayPark.getArrivalTime());
        dto.setDepartureTime(dayPark.getDepartureTime());
        dto.setNotes(dayPark.getNotes());
        dto.setCreatedAt(dayPark.getCreatedAt());
        return dto;
    }
}
