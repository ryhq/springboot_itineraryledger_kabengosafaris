package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Repository.SafariDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.CreateSafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.SafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkActivityCreateService - Service for creating park activities within a safari park visit
 *
 * Note: sortOrder is auto-assigned based on existing activities.
 * Duplicate activities are allowed (e.g., morning and evening game drives).
 */
@Service
@Slf4j
@Transactional
public class SafariDayParkActivityCreateService {

    private final SafariDayParkRepository dayParkRepository;
    private final SafariDayParkActivityRepository parkActivityRepository;
    private final ParkActivityRepository baseParkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkActivityCreateService(
        SafariDayParkRepository dayParkRepository,
        SafariDayParkActivityRepository parkActivityRepository,
        ParkActivityRepository baseParkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.dayParkRepository = dayParkRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.baseParkActivityRepository = baseParkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Add activities to a safari park visit
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param createDTOs List of activities to add
     * @return ResponseEntity with ApiResponse
     */
    @AuditLogAnnotation(action = "ADD_SAFARI_PARK_ACTIVITIES", description = "Adding activities to safari park visit", entityType = "SafariDayParkActivity")
    public ResponseEntity<ApiResponse<?>> addParkActivities(
        String parkVisitIdObfuscated,
        List<CreateSafariDayParkActivityDTO> createDTOs
    ) {
        log.info("Adding {} activities to safari park visit: {}", createDTOs.size(), parkVisitIdObfuscated);

        try {
            // Decode park visit ID
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            // Find park visit
            SafariDayPark parkVisit = dayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari park visit not found", "SAFARI_PARK_VISIT_NOT_FOUND")
                );
            }

            Long parkId = parkVisit.getPark().getId();

            // Get current max sortOrder
            int currentMaxSortOrder = parkActivityRepository.findBySafariDayParkIdOrderBySortOrderAsc(parkVisitId)
                .stream()
                .mapToInt(SafariDayParkActivity::getSortOrder)
                .max()
                .orElse(0);

            List<SafariDayParkActivityDTO> resultDTOs = new ArrayList<>();

            for (CreateSafariDayParkActivityDTO dto : createDTOs) {
                try {
                    // Decode activity ID
                    Long activityId = idObfuscator.decodeId(dto.getActivityId());

                    // Verify parkId matches (must be same park)
                    Long dtoParkId = idObfuscator.decodeId(dto.getParkId());
                    if (!dtoParkId.equals(parkId)) {
                        log.warn("Park ID mismatch for activity: expected {}, got {}", parkId, dtoParkId);
                        continue;
                    }

                    // Find ParkActivity by park and activity IDs
                    ParkActivity parkActivity = baseParkActivityRepository.findByParkIdAndActivityId(parkId, activityId).orElse(null);
                    if (parkActivity == null) {
                        log.warn("ParkActivity not found: park={}, activity={}", parkId, activityId);
                        continue;
                    }

                    // Auto-assign sortOrder (handled by reorder methods)
                    int sortOrder = ++currentMaxSortOrder;

                    // Create entry
                    SafariDayParkActivity entry = SafariDayParkActivity.builder()
                        .safariDayPark(parkVisit)
                        .parkActivity(parkActivity)
                        .sortOrder(sortOrder)
                        .durationHours(dto.getDurationHours())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .notes(dto.getNotes())
                        .isIncludedInPrice(dto.getIsIncludedInPrice() != null ? dto.getIsIncludedInPrice() : true)
                        .build();

                    entry = parkActivityRepository.save(entry);
                    resultDTOs.add(convertToDTO(entry));

                } catch (Exception e) {
                    log.error("Error adding safari park activity", e);
                }
            }

            log.info("Added {} safari park activities", resultDTOs.size());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, resultDTOs.size() + " activities added", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error adding safari park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to add safari park activities", "SAFARI_PARK_ACTIVITIES_ADD_FAILED")
            );
        }
    }

    private SafariDayParkActivityDTO convertToDTO(SafariDayParkActivity entity) {
        SafariDayParkActivityDTO dto = new SafariDayParkActivityDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setSafariDayParkId(idObfuscator.encodeId(entity.getSafariDayPark().getId()));
        dto.setParkId(idObfuscator.encodeId(entity.getParkActivity().getPark().getId()));
        dto.setParkName(entity.getParkActivity().getPark().getName());
        dto.setActivityId(idObfuscator.encodeId(entity.getParkActivity().getActivity().getId()));
        dto.setActivityName(entity.getParkActivity().getActivity().getName());
        dto.setSortOrder(entity.getSortOrder());
        dto.setDurationHours(entity.getDurationHours());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setNotes(entity.getNotes());
        dto.setIsIncludedInPrice(entity.getIsIncludedInPrice());

        // Safari-specific fields
        dto.setIsCompleted(entity.getIsCompleted());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setActualDurationHours(entity.getActualDurationHours());
        dto.setSightingsNotes(entity.getSightingsNotes());
        dto.setGuestExperience(entity.getGuestExperience());
        dto.setIsSkipped(entity.getIsSkipped());
        dto.setSkipReason(entity.getSkipReason());

        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
