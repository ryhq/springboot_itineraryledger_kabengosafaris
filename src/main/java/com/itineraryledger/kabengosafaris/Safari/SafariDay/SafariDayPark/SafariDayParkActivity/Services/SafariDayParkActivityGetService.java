package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Repository.SafariDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.DTOs.SafariDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariDayParkActivityGetService - Service for retrieving park activities within a safari park visit
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariDayParkActivityGetService {

    private final SafariDayParkActivityRepository parkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariDayParkActivityGetService(
        SafariDayParkActivityRepository parkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.parkActivityRepository = parkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get all activities for a park visit
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @return ResponseEntity with ApiResponse containing list of activities
     */
    public ResponseEntity<ApiResponse<?>> getParkActivities(String parkVisitIdObfuscated) {
        log.info("Fetching activities for safari park visit: {}", parkVisitIdObfuscated);

        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            List<SafariDayParkActivity> activities = parkActivityRepository.findBySafariDayParkIdOrderBySortOrderAsc(parkVisitId);
            List<SafariDayParkActivityDTO> dtos = activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari park activities retrieved", dtos)
            );

        } catch (Exception e) {
            log.error("Error fetching safari park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari park activities", "FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single activity by ID
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param activityIdObfuscated The obfuscated activity entry ID
     * @return ResponseEntity with ApiResponse containing the activity
     */
    public ResponseEntity<ApiResponse<?>> getParkActivity(
        String parkVisitIdObfuscated,
        String activityIdObfuscated
    ) {
        log.info("Fetching activity {} for safari park visit: {}", activityIdObfuscated, parkVisitIdObfuscated);

        try {
            Long parkVisitId;
            Long activityId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
                activityId = idObfuscator.decodeId(activityIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid ID", "INVALID_ID")
                );
            }

            SafariDayParkActivity activity = parkActivityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari park activity not found", "SAFARI_PARK_ACTIVITY_NOT_FOUND")
                );
            }

            // Verify ownership
            if (!activity.getSafariDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity does not belong to this park visit", "OWNERSHIP_MISMATCH")
                );
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Safari park activity retrieved", convertToDTO(activity))
            );

        } catch (Exception e) {
            log.error("Error fetching safari park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch safari park activity", "FETCH_FAILED")
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
