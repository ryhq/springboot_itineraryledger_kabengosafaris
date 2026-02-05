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
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.CreateSafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.SafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkCreateService - Service for creating safari day park visits
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkCreateService {

    private final SafariDayRepository safariDayRepository;
    private final SafariDayParkRepository safariDayParkRepository;
    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkCreateService(
        SafariDayRepository safariDayRepository,
        SafariDayParkRepository safariDayParkRepository,
        ParkRepository parkRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayRepository = safariDayRepository;
        this.safariDayParkRepository = safariDayParkRepository;
        this.parkRepository = parkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new park visit for a safari day
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param createDTO The park visit data
     * @return ResponseEntity with ApiResponse containing the created park visit
     */
    @AuditLogAnnotation(action = "CREATE_SAFARI_DAY_PARK", description = "Creating park visit for safari day", entityType = "SafariDayPark")
    public ResponseEntity<ApiResponse<?>> createSafariDayPark(
        String safariIdObfuscated,
        String dayIdObfuscated,
        CreateSafariDayParkDTO createDTO
    ) {
        log.info("Creating park visit for day: {}", dayIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long parkId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                parkId = idObfuscator.decodeId(createDTO.getParkId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            // Find day
            SafariDay day = safariDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari day not found", "SAFARI_DAY_NOT_FOUND")
                );
            }

            // Verify day belongs to the safari
            if (!day.getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
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
            if (safariDayParkRepository.existsBySafariDayIdAndParkId(dayId, parkId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Park '" + park.getName() + "' is already added to this day",
                        "PARK_ALREADY_EXISTS"
                    )
                );
            }

            // Auto-determine sortOrder based on existing park visits count
            long existingCount = safariDayParkRepository.countBySafariDayId(dayId);
            int nextSortOrder = (int) existingCount + 1;

            // Create park visit
            SafariDayPark dayPark = SafariDayPark.builder()
                .safariDay(day)
                .park(park)
                .entryType(createDTO.getEntryType())
                .sortOrder(nextSortOrder)
                .arrivalTime(createDTO.getArrivalTime())
                .departureTime(createDTO.getDepartureTime())
                .notes(createDTO.getNotes())
                .build();

            // Save
            dayPark = safariDayParkRepository.save(dayPark);

            // Convert to DTO
            SafariDayParkDTO dto = convertToDTO(dayPark);

            log.info("Park visit created successfully: {} for day {}", park.getName(), day.getDayNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Park visit created successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error creating park visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create park visit", "SAFARI_DAY_PARK_CREATE_FAILED")
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
