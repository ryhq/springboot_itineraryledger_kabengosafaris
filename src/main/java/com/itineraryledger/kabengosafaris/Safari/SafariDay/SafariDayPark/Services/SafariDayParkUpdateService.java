package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.SafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.DTOs.UpdateSafariDayParkDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkUpdateService - Service for updating safari day park visits
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkUpdateService {

    private final SafariDayParkRepository safariDayParkRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkUpdateService(
        SafariDayParkRepository safariDayParkRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariDayParkRepository = safariDayParkRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update a safari day park visit
     *
     * @param safariIdObfuscated The obfuscated safari ID
     * @param dayIdObfuscated The obfuscated day ID
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param updateDTO The update data
     * @return ResponseEntity with ApiResponse containing the updated park visit
     */
    @AuditLogAnnotation(action = "UPDATE_SAFARI_DAY_PARK", description = "Updating park visit", entityType = "SafariDayPark")
    public ResponseEntity<ApiResponse<?>> updateSafariDayPark(
        String safariIdObfuscated,
        String dayIdObfuscated,
        String parkVisitIdObfuscated,
        UpdateSafariDayParkDTO updateDTO
    ) {
        log.info("Updating park visit: {}", parkVisitIdObfuscated);

        try {
            // Decode IDs
            Long safariId;
            Long dayId;
            Long parkVisitId;
            try {
                safariId = idObfuscator.decodeId(safariIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID format", "INVALID_ID")
                );
            }

            // Find park visit
            SafariDayPark parkVisit = safariDayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            // Verify ownership chain
            if (!parkVisit.getSafariDay().getId().equals(dayId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park visit does not belong to this day", "PARK_VISIT_DAY_MISMATCH")
                );
            }

            if (!parkVisit.getSafariDay().getSafari().getId().equals(safariId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Day does not belong to this safari", "DAY_SAFARI_MISMATCH")
                );
            }

            // Update fields if provided
            if (updateDTO.getEntryType() != null) {
                parkVisit.setEntryType(updateDTO.getEntryType());
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

            // Update Safari-specific fields
            if (updateDTO.getActualArrivalTime() != null) {
                parkVisit.setActualArrivalTime(updateDTO.getActualArrivalTime());
            }
            if (updateDTO.getActualDepartureTime() != null) {
                parkVisit.setActualDepartureTime(updateDTO.getActualDepartureTime());
            }
            if (updateDTO.getEntryReceiptNumber() != null) {
                parkVisit.setEntryReceiptNumber(updateDTO.getEntryReceiptNumber());
            }
            if (updateDTO.getWildlifeSightings() != null) {
                parkVisit.setWildlifeSightings(updateDTO.getWildlifeSightings());
            }
            if (updateDTO.getVisitNotes() != null) {
                parkVisit.setVisitNotes(updateDTO.getVisitNotes());
            }
            if (updateDTO.getFeesPaid() != null) {
                parkVisit.setFeesPaid(updateDTO.getFeesPaid());
            }
            if (updateDTO.getFeesPaidAt() != null) {
                parkVisit.setFeesPaidAt(updateDTO.getFeesPaidAt());
            }
            if (updateDTO.getWeatherConditions() != null) {
                parkVisit.setWeatherConditions(updateDTO.getWeatherConditions());
            }

            // Save
            parkVisit = safariDayParkRepository.save(parkVisit);

            // Convert to DTO
            SafariDayParkDTO dto = convertToDTO(parkVisit);

            log.info("Park visit updated successfully: {}", parkVisitId);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park visit updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating park visit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update park visit", "SAFARI_DAY_PARK_UPDATE_FAILED")
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
