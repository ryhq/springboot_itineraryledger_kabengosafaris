package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.CreateItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.DTOs.ItineraryDayParkDTO;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkCreateService - Service for creating itinerary day park visits
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayParkCreateService {

    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryDayParkRepository itineraryDayParkRepository;
    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkCreateService(
        ItineraryDayRepository itineraryDayRepository,
        ItineraryDayParkRepository itineraryDayParkRepository,
        ParkRepository parkRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryDayParkRepository = itineraryDayParkRepository;
        this.parkRepository = parkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new park visit for an itinerary day
     *
     * @param itineraryIdObfuscated The obfuscated itinerary ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param createDTO The park visit data
     * @return ResponseEntity with ApiResponse containing the created park visit
     */
    @AuditLogAnnotation(action = "CREATE_ITINERARY_DAY_PARK", description = "Creating park visit for itinerary day", entityType = "ItineraryDayPark")
    public ResponseEntity<ApiResponse<?>> createItineraryDayPark(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        CreateItineraryDayParkDTO createDTO
    ) {
        log.info("Creating park visit for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long itineraryId;
            Long dayId;
            Long parkId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                parkId = idObfuscator.decodeId(createDTO.getParkId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find day
            ItineraryDay day = itineraryDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary day not found", "ITINERARY_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to the itinerary
            if (!day.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this itinerary", "DAY_ITINERARY_MISMATCH")
                );
            }

            // Find park
            Park park = parkRepository.findById(parkId).orElse(null);
            if (park == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND")
                );
            }

            // Check if park already exists for this day
            if (itineraryDayParkRepository.existsByItineraryDayIdAndParkId(dayId, parkId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park '" + park.getName() + "' is already added to this day",
                        "PARK_ALREADY_EXISTS"
                    )
                );
            }

            // Create park visit
            ItineraryDayPark dayPark = ItineraryDayPark.builder()
                .itineraryDay(day)
                .park(park)
                .entryType(createDTO.getEntryType())
                .sortOrder(createDTO.getSortOrder() != null ? createDTO.getSortOrder() : 0)
                .arrivalTime(createDTO.getArrivalTime())
                .departureTime(createDTO.getDepartureTime())
                .notes(createDTO.getNotes())
                .build();

            // Save
            dayPark = itineraryDayParkRepository.save(dayPark);

            // Convert to DTO
            ItineraryDayParkDTO dto = convertToDTO(dayPark);

            log.info("Park visit created successfully: {} for day {}", park.getName(), day.getDayNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Park visit created successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error creating park visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create park visit", "ITINERARY_DAY_PARK_CREATE_FAILED")
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
