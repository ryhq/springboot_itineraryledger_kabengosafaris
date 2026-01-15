package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.ItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkGetService - Service for retrieving itinerary day park visits
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayParkGetService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryDayParkRepository itineraryDayParkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkGetService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayParkRepository itineraryDayParkRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryDayParkRepository = itineraryDayParkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all park visits for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @return ResponseEntity with ApiResponse containing list of park visits
     */
    public ResponseEntity<ApiResponse<?>> getItineraryDayParks(String itineraryIdObfuscated, String dayIdObfuscated) {
        log.info("Fetching parks for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Verify day exists and belongs to itinerary
            var day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Fetch park visits
            List<ItineraryDayPark> parks = itineraryDayParkRepository.findByItineraryDayIdOrderBySortOrderAsc(dayId);

            // Convert to DTOs
            List<ItineraryDayParkDTO> parkDTOs = parks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visits retrieved successfully", parkDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching park visits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park visits", "ITINERARY_DAY_PARKS_FETCH_FAILED")
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
