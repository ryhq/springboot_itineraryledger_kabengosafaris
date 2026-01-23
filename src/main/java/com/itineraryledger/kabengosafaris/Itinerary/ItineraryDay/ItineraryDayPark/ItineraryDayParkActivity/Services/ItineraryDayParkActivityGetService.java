package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository.ItineraryDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkActivityGetService - Service for retrieving park activities within a park visit
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ItineraryDayParkActivityGetService {

    private final ItineraryDayParkActivityRepository parkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkActivityGetService(
        ItineraryDayParkActivityRepository parkActivityRepository,
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
        log.info("Fetching activities for park visit: {}", parkVisitIdObfuscated);

        try {
            Long parkVisitId;
            try {
                parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid park visit ID", "INVALID_ID")
                );
            }

            List<ItineraryDayParkActivity> activities = parkActivityRepository.findByItineraryDayParkIdOrderBySortOrderAsc(parkVisitId);
            List<ItineraryDayParkActivityDTO> dtos = activities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activities retrieved", dtos)
            );

        } catch (Exception e) {
            log.error("Error fetching park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park activities", "FETCH_FAILED")
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
        log.info("Fetching activity {} for park visit: {}", activityIdObfuscated, parkVisitIdObfuscated);

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

            ItineraryDayParkActivity activity = parkActivityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park activity not found", "PARK_ACTIVITY_NOT_FOUND")
                );
            }

            // Verify ownership
            if (!activity.getItineraryDayPark().getId().equals(parkVisitId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity does not belong to this park visit", "OWNERSHIP_MISMATCH")
                );
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Park activity retrieved", convertToDTO(activity))
            );

        } catch (Exception e) {
            log.error("Error fetching park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch park activity", "FETCH_FAILED")
            );
        }
    }

    private ItineraryDayParkActivityDTO convertToDTO(ItineraryDayParkActivity entity) {
        ItineraryDayParkActivityDTO dto = new ItineraryDayParkActivityDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setItineraryDayParkId(idObfuscator.encodeId(entity.getItineraryDayPark().getId()));
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
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
