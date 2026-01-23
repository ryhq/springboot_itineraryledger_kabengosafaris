package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository.ItineraryDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.CreateItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.DTOs.ItineraryDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryDayParkActivityService - Service for managing park activities within a park visit
 */
@Service
@Slf4j
@Transactional
public class ItineraryDayParkActivityService {

    private final ItineraryDayParkRepository dayParkRepository;
    private final ItineraryDayParkActivityRepository parkActivityRepository;
    private final ParkActivityRepository baseParkActivityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryDayParkActivityService(
        ItineraryDayParkRepository dayParkRepository,
        ItineraryDayParkActivityRepository parkActivityRepository,
        ParkActivityRepository baseParkActivityRepository,
        IdObfuscator idObfuscator
    ) {
        this.dayParkRepository = dayParkRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.baseParkActivityRepository = baseParkActivityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Add activities to a park visit
     *
     * @param parkVisitIdObfuscated The obfuscated park visit ID
     * @param createDTOs List of activities to add
     * @return ResponseEntity with ApiResponse
     */
    @AuditLogAnnotation(action = "ADD_PARK_ACTIVITIES", description = "Adding activities to park visit", entityType = "ItineraryDayParkActivity")
    public ResponseEntity<ApiResponse<?>> addParkActivities(
        String parkVisitIdObfuscated,
        List<CreateItineraryDayParkActivityDTO> createDTOs
    ) {
        log.info("Adding {} activities to park visit: {}", createDTOs.size(), parkVisitIdObfuscated);

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
            ItineraryDayPark parkVisit = dayParkRepository.findById(parkVisitId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Park visit not found", "PARK_VISIT_NOT_FOUND")
                );
            }

            Long parkId = parkVisit.getPark().getId();
            List<ItineraryDayParkActivityDTO> resultDTOs = new ArrayList<>();

            // Get current max sortOrder
            int currentMaxSortOrder = parkActivityRepository.findByItineraryDayParkIdOrderBySortOrderAsc(parkVisitId)
                .stream()
                .mapToInt(ItineraryDayParkActivity::getSortOrder)
                .max()
                .orElse(0);

            for (CreateItineraryDayParkActivityDTO dto : createDTOs) {
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
                    ItineraryDayParkActivity entry = ItineraryDayParkActivity.builder()
                        .itineraryDayPark(parkVisit)
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
                    log.error("Error adding park activity", e);
                }
            }

            log.info("Added {} park activities", resultDTOs.size());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, resultDTOs.size() + " activities added", resultDTOs)
            );

        } catch (Exception e) {
            log.error("Error adding park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to add park activities", "PARK_ACTIVITIES_ADD_FAILED")
            );
        }
    }

    /**
     * Get all activities for a park visit
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getParkActivities(String parkVisitIdObfuscated) {
        log.info("Fetching activities for park visit: {}", parkVisitIdObfuscated);

        try {
            Long parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);

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
     * Delete park activities
     */
    public ResponseEntity<ApiResponse<?>> deleteParkActivities(
        String parkVisitIdObfuscated,
        List<String> activityIdObfuscatedList
    ) {
        log.info("Deleting {} park activities", activityIdObfuscatedList.size());

        try {
            Long parkVisitId = idObfuscator.decodeId(parkVisitIdObfuscated);

            int deletedCount = 0;
            for (String idObfuscated : activityIdObfuscatedList) {
                try {
                    Long activityEntryId = idObfuscator.decodeId(idObfuscated);
                    ItineraryDayParkActivity entry = parkActivityRepository.findById(activityEntryId).orElse(null);

                    if (entry == null || !entry.getItineraryDayPark().getId().equals(parkVisitId)) {
                        continue;
                    }

                    ((ItineraryDayParkActivityService) AopContext.currentProxy()).deleteActivity(activityEntryId);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Error deleting park activity", e);
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " activities deleted", null)
            );

        } catch (Exception e) {
            log.error("Error deleting park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete park activities", "DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_PARK_ACTIVITY", description = "Deleting park activity", entityType = "ItineraryDayParkActivity", entityIdParamName = "id")
    public void deleteActivity(Long id) {
        parkActivityRepository.deleteById(id);
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
