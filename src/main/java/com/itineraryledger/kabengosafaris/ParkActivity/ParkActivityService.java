package com.itineraryledger.kabengosafaris.ParkActivity;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivitySpecification;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityWithNotesDTO;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkWithNotesDTO;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityUpsertDTO;
import com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityUpsertResponseDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing park-activity associations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkActivityService {

    private final ParkActivityRepository parkActivityRepository;
    private final ParkRepository parkRepository;
    private final ActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    /**
     * Get all activities for a specific park with filtering, pagination, and sorting
     */
    public ResponseEntity<ApiResponse<?>> getActivitiesForPark(
        String parkIdObfuscated,
        Boolean assigned,
        String name,
        String slug,
        Boolean hasTariff,
        Boolean isWebActive,
        ChargingBasis chargingBasis,
        Boolean isActive,
        String keyword,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching activities for park: {}, assigned: {}", parkIdObfuscated, assigned);

        try {
            // Decode park ID
            Long parkId = idObfuscator.decodeId(parkIdObfuscated);

            // Verify park exists
            if (!parkRepository.existsById(parkId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Park not found", "PARK_NOT_FOUND")
                );
            }

            // Build specification - filter by park assignment
            Specification<Activity> spec;
            if (assigned != null && !assigned) {
                // Get activities NOT assigned to this park
                spec = ActivitySpecification.notByParkId(parkId);
            } else {
                // Get activities assigned to this park (default behavior)
                spec = ActivitySpecification.byParkId(parkId);
            }

            // Add additional filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(ActivitySpecification.nameLike(name));
            }
            if (slug != null && !slug.isEmpty()) {
                spec = spec.and(ActivitySpecification.slugLike(slug));
            }
            if (hasTariff != null) {
                spec = spec.and(ActivitySpecification.hasTariff(hasTariff));
            }
            if (isWebActive != null) {
                spec = spec.and(ActivitySpecification.isWebActive(isWebActive));
            }
            if (chargingBasis != null) {
                spec = spec.and(ActivitySpecification.hasChargingBasis(chargingBasis));
            }
            if (isActive != null) {
                spec = spec.and(ActivitySpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(ActivitySpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));

            // Fetch activities
            Page<Activity> activityPage = activityRepository.findAll(spec, pageable);

            // Convert to DTOs with notes (only include notes if assigned=true)
            boolean includeNotes = assigned == null || assigned;
            List<ActivityWithNotesDTO> activityDTOs = activityPage.getContent().stream()
                .map(activity -> convertActivityToWithNotesDTO(activity, includeNotes ? parkId : null))
                .collect(Collectors.toList());

            // Response with pagination
            Map<String, Object> response = new HashMap<>();
            response.put("activities", activityDTOs);
            response.put("currentPage", activityPage.getNumber());
            response.put("totalItems", activityPage.getTotalElements());
            response.put("totalPages", activityPage.getTotalPages());

            return ResponseEntity.ok(ApiResponse.success(200, "Activities retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching activities for park", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to fetch activities", "FETCH_FAILED")
            );
        }
    }

    /**
     * Get all parks for a specific activity with filtering, pagination, and sorting
     */
    public ResponseEntity<ApiResponse<?>> getParksForActivity(
        String activityIdObfuscated,
        Boolean assigned,
        String name,
        String slug,
        ParkType parkType,
        String region,
        String district,
        String location,
        String parkSize,
        Boolean isActive,
        String keyword,
        Integer page,
        Integer size,
        String sortDirection
    ) {
        log.info("Fetching parks for activity: {}, assigned: {}", activityIdObfuscated, assigned);

        try {
            // Decode activity ID
            Long activityId = idObfuscator.decodeId(activityIdObfuscated);

            // Verify activity exists
            if (!activityRepository.existsById(activityId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Activity not found", "ACTIVITY_NOT_FOUND")
                );
            }

            // Build specification - filter by activity assignment
            Specification<Park> spec;
            if (assigned != null && !assigned) {
                // Get parks NOT assigned to this activity
                spec = ParkSpecification.notByActivityId(activityId);
            } else {
                // Get parks assigned to this activity (default behavior)
                spec = ParkSpecification.byActivityId(activityId);
            }

            // Add additional filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(ParkSpecification.nameLike(name));
            }
            if (slug != null && !slug.isEmpty()) {
                spec = spec.and(ParkSpecification.slugLike(slug));
            }
            if (parkType != null) {
                spec = spec.and(ParkSpecification.hasParkType(parkType));
            }
            if (region != null && !region.isEmpty()) {
                spec = spec.and(ParkSpecification.regionLike(region));
            }
            if (district != null && !district.isEmpty()) {
                spec = spec.and(ParkSpecification.districtLike(district));
            }
            if (location != null && !location.isEmpty()) {
                spec = spec.and(ParkSpecification.locationLike(location));
            }
            if (parkSize != null && !parkSize.isEmpty()) {
                spec = spec.and(ParkSpecification.sizeLike(parkSize));
            }
            if (isActive != null) {
                spec = spec.and(ParkSpecification.isActive(isActive));
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(ParkSpecification.searchKeyword(keyword));
            }

            // Pagination
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));

            // Fetch parks
            Page<Park> parkPage = parkRepository.findAll(spec, pageable);

            // Convert to DTOs with notes (only include notes if assigned=true)
            boolean includeNotes = assigned == null || assigned;
            List<ParkWithNotesDTO> parkDTOs = parkPage.getContent().stream()
                .map(park -> convertParkToWithNotesDTO(park, includeNotes ? activityId : null))
                .collect(Collectors.toList());

            // Response with pagination
            Map<String, Object> response = new HashMap<>();
            response.put("parks", parkDTOs);
            response.put("currentPage", parkPage.getNumber());
            response.put("totalItems", parkPage.getTotalElements());
            response.put("totalPages", parkPage.getTotalPages());

            return ResponseEntity.ok(ApiResponse.success(200, "Parks retrieved successfully", response));

        } catch (Exception e) {
            log.error("Error fetching parks for activity", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to fetch parks", "FETCH_FAILED")
            );
        }
    }

    /**
     * Bulk upsert park-activity relationships
     */
    @Transactional
    @AuditLogAnnotation(action = "UPSERT_PARK_ACTIVITIES", description = "Bulk upsert park-activity relationships", entityType = "ParkActivity")
    public ResponseEntity<ApiResponse<?>> upsertParkActivities(List<ParkActivityUpsertDTO> requests) {
        log.info("Processing bulk upsert for {} park-activity relationships", requests.size());

        ParkActivityUpsertResponseDTO response = new ParkActivityUpsertResponseDTO();
        response.setTotalProcessed(requests.size());

        for (ParkActivityUpsertDTO request : requests) {
            try {
                // Decode IDs
                Long parkId;
                Long activityId;

                try {
                    parkId = idObfuscator.decodeId(request.getParkId());
                    activityId = idObfuscator.decodeId(request.getActivityId());
                } catch (Exception e) {
                    response.addError("Invalid ID format for park: " + request.getParkId() + ", activity: " + request.getActivityId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify park exists
                Park park = parkRepository.findById(parkId).orElse(null);
                if (park == null) {
                    response.addError("Park not found: " + request.getParkId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                // Verify activity exists
                Activity activity = activityRepository.findById(activityId).orElse(null);
                if (activity == null) {
                    response.addError("Activity not found: " + request.getActivityId());
                    response.setFailed(response.getFailed() + 1);
                    continue;
                }

                if (request.getStatus()) {
                    // Create or update relationship
                    ParkActivity parkActivity = parkActivityRepository.findByParkIdAndActivityId(parkId, activityId).orElse(null);

                    if (parkActivity != null) {
                        // Update existing
                        parkActivity.setNotes(request.getNotes());
                        parkActivityRepository.save(parkActivity);
                        log.info("Updated park-activity relationship: park={}, activity={}", park.getName(), activity.getName());
                    } else {
                        // Create new
                        parkActivity = ParkActivity.builder()
                            .park(park)
                            .activity(activity)
                            .notes(request.getNotes())
                            .build();
                        parkActivityRepository.save(parkActivity);
                        log.info("Created park-activity relationship: park={}, activity={}", park.getName(), activity.getName());
                    }

                    response.setSuccessful(response.getSuccessful() + 1);

                } else {
                    // Delete relationship
                    ParkActivity parkActivity = parkActivityRepository.findByParkIdAndActivityId(parkId, activityId).orElse(null);

                    if (parkActivity == null) {
                        response.addError("Relationship does not exist for park: " + park.getName() + ", activity: " + activity.getName());
                        response.setFailed(response.getFailed() + 1);
                    } else {
                        parkActivityRepository.delete(parkActivity);
                        log.info("Deleted park-activity relationship: park={}, activity={}", park.getName(), activity.getName());
                        response.setSuccessful(response.getSuccessful() + 1);
                    }
                }

            } catch (Exception e) {
                log.error("Error processing park-activity upsert", e);
                response.addError("Error processing relationship: " + e.getMessage());
                response.setFailed(response.getFailed() + 1);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(200, "Bulk upsert completed", response));
    }

    /**
     * Convert Activity to DTO with park-specific notes
     */
    private ActivityWithNotesDTO convertActivityToWithNotesDTO(Activity activity, Long parkId) {
        ActivityWithNotesDTO dto = new ActivityWithNotesDTO();
        dto.setId(idObfuscator.encodeId(activity.getId()));
        dto.setName(activity.getName());
        dto.setSlug(activity.getSlug());
        dto.setHasTariff(activity.getHasTariff());
        dto.setIsWebActive(activity.getIsWebActive());
        dto.setChargingBasis(activity.getChargingBasis());
        dto.setDescription(activity.getDescription());
        dto.setDetailedDescription(activity.getDetailedDescription());
        dto.setMinimumAge(activity.getMinimumAge());
        dto.setMaximumParticipants(activity.getMaximumParticipants());
        dto.setEquipmentRequired(activity.getEquipmentRequired());
        dto.setSeasonAvailability(activity.getSeasonAvailability());
        dto.setPrimaryImage(activity.getPrimaryImage());
        dto.setTags(activity.getTags());
        dto.setSafetyInformation(activity.getSafetyInformation());
        dto.setIsActive(activity.getIsActive());
        dto.setCreatedAt(activity.getCreatedAt());
        dto.setUpdatedAt(activity.getUpdatedAt());

        // Get notes from ParkActivity relationship (only if parkId is provided)
        if (parkId != null) {
            activity.getParkActivities().stream()
                .filter(pa -> pa.getPark().getId().equals(parkId))
                .findFirst()
                .ifPresent(pa -> dto.setNotes(pa.getNotes()));
        }

        return dto;
    }

    /**
     * Convert Park to DTO with activity-specific notes
     */
    private ParkWithNotesDTO convertParkToWithNotesDTO(Park park, Long activityId) {
        ParkWithNotesDTO dto = new ParkWithNotesDTO();
        dto.setId(idObfuscator.encodeId(park.getId()));
        dto.setName(park.getName());
        dto.setSlug(park.getSlug());
        dto.setParkType(park.getParkType());
        dto.setRegion(park.getRegion());
        dto.setDistrict(park.getDistrict());
        dto.setLocation(park.getLocation());
        dto.setLatitude(park.getLatitude());
        dto.setLongitude(park.getLongitude());
        dto.setElevation(park.getElevation());
        dto.setSize(park.getSize());
        dto.setShortDescription(park.getShortDescription());
        dto.setFullDescription(park.getFullDescription());
        dto.setHistory(park.getHistory());
        dto.setEcosystem(park.getEcosystem());
        dto.setWildlife(park.getWildlife());
        dto.setVegetation(park.getVegetation());
        dto.setPrimaryImage(park.getPrimaryImage());
        dto.setBestTimeToVisit(park.getBestTimeToVisit());
        dto.setOpeningHours(park.getOpeningHours());
        dto.setAccessInformation(park.getAccessInformation());
        dto.setTags(park.getTags());
        dto.setIsActive(park.getIsActive());
        dto.setCreatedAt(park.getCreatedAt());
        dto.setUpdatedAt(park.getUpdatedAt());

        // Get notes from ParkActivity relationship (only if activityId is provided)
        if (activityId != null) {
            park.getParkActivities().stream()
                .filter(pa -> pa.getActivity().getId().equals(activityId))
                .findFirst()
                .ifPresent(pa -> dto.setNotes(pa.getNotes()));
        }

        return dto;
    }
}
