package com.itineraryledger.kabengosafaris.Activity.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivitySpecification;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ActivityGetService - Service for retrieving activities with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ActivityGetService {

    private final ActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityGetService(
        ActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single activity by obfuscated ID
     *
     * @param idObfuscated The obfuscated activity ID
     * @return ResponseEntity with ApiResponse containing the activity
     */
    public ResponseEntity<ApiResponse<?>> getActivityById(String idObfuscated) {
        log.info("Fetching activity with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode activity ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid activity ID",
                        "INVALID_ACTIVITY_ID"
                    )
                );
            }

            // Find activity
            Activity activity = activityRepository.findById(id).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Activity not found",
                        "ACTIVITY_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            ActivityDTO activityDTO = convertToDTO(activity);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Activity retrieved successfully",
                    activityDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch activity",
                    "ACTIVITY_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get a single activity by slug
     *
     * @param slug The activity slug
     * @return ResponseEntity with ApiResponse containing the activity
     */
    public ResponseEntity<ApiResponse<?>> getActivityBySlug(String slug) {
        log.info("Fetching activity with slug: {}", slug);

        try {
            // Find activity
            Activity activity = activityRepository.findBySlug(slug).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Activity not found",
                        "ACTIVITY_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            ActivityDTO activityDTO = convertToDTO(activity);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Activity retrieved successfully",
                    activityDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch activity",
                    "ACTIVITY_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all activities with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param hasTariff Filter by tariff status
     * @param isWebActive Filter by web active status
     * @param chargingBasis Filter by charging basis
     * @param isActive Filter by active status
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated activities
     */
    public ResponseEntity<ApiResponse<?>> getAllActivities(
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
        log.info("Fetching all activities with filters");

        try {
            // Build specification for filtering
            Specification<Activity> spec = Specification.unrestricted();

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

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Set default sorting (always by createdAt)
            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));

            // Fetch activities
            Page<Activity> activityPage = activityRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<ActivityDTO> activityDTOs = activityPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("activities", activityDTOs);
            response.put("currentPage", activityPage.getNumber());
            response.put("totalItems", activityPage.getTotalElements());
            response.put("totalPages", activityPage.getTotalPages());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Activities retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch activities",
                    "ACTIVITIES_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert Activity entity to ActivityDTO
     */
    private ActivityDTO convertToDTO(Activity activity) {
        ActivityDTO dto = new ActivityDTO();
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
        return dto;
    }
}
