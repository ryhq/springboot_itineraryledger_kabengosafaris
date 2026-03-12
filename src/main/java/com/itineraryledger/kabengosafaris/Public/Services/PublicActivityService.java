package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivitySpecification;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicActivityDetailDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicActivityListDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicImageDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicParkListDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityImageRepository activityImageRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final PublicEntityResolver entityResolver;
    private final PublicImageResolver imageResolver;
    private final IdObfuscator idObfuscator;
    private final PublicTranslationService publicTranslationService;

    public ResponseEntity<ApiResponse<?>> getActivities(Integer page, Integer size, String sortBy, String sortDirection,
                                                         String keyword, String lang) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDirection = sortDirection != null ? sortDirection : "asc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "name";

            Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Activity> spec = ActivitySpecification.isActive(true)
                .and(ActivitySpecification.isWebActive(true));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ActivitySpecification.searchKeyword(keyword));

            Page<Activity> activityPage = activityRepository.findAll(spec, pageable);
            List<PublicActivityListDTO> dtos = activityPage.getContent().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());

            publicTranslationService.translateDtoList(dtos, lang);

            return ResponseEntity.ok(ApiResponse.success(200, "Activities retrieved", PublicServiceUtils.buildPageResponse("activities", dtos, activityPage)));
        } catch (Exception e) {
            log.error("Error fetching public activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch activities", "ACTIVITIES_FETCH_FAILED"));
        }
    }

    /**
     * Get activity by identifier (obfuscated id or slug)
     */
    public ResponseEntity<ApiResponse<?>> getActivityByIdentifier(String identifier, String lang) {
        try {
            Activity activity = entityResolver.resolveActivity(identifier).orElse(null);
            if (activity == null || !Boolean.TRUE.equals(activity.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
            }
            return buildActivityDetailResponse(activity, lang);
        } catch (Exception e) {
            log.error("Error fetching activity by identifier: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch activity", "ACTIVITY_FETCH_FAILED"));
        }
    }

    /**
     * Get paginated images for an activity (by identifier)
     */
    public ResponseEntity<ApiResponse<?>> getActivityImages(String identifier, Integer page, Integer size) {
        try {
            Activity activity = entityResolver.resolveActivity(identifier).orElse(null);
            if (activity == null || !Boolean.TRUE.equals(activity.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
            }

            page = page != null ? page : 0;
            size = size != null ? size : 6;
            Pageable pageable = PageRequest.of(page, size);

            Page<ActivityImage> imagePage = activityImageRepository.findActiveByActivityIdPaginated(activity.getId(), pageable);
            List<PublicImageDTO> dtos = imagePage.getContent().stream()
                .map(imageResolver::toPublicDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Activity images retrieved", PublicServiceUtils.buildPageResponse("images", dtos, imagePage)));
        } catch (Exception e) {
            log.error("Error fetching activity images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch activity images", "ACTIVITY_IMAGES_FETCH_FAILED"));
        }
    }

    /**
     * Get parks that offer an activity (by identifier)
     */
    public ResponseEntity<ApiResponse<?>> getActivityParks(String identifier, Integer page, Integer size, String lang) {
        try {
            Activity activity = entityResolver.resolveActivity(identifier).orElse(null);
            if (activity == null || !Boolean.TRUE.equals(activity.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
            }

            page = page != null ? page : 0;
            size = size != null ? size : 6;

            List<ParkActivity> parkActivities = parkActivityRepository.findByActivityIdWithPark(activity.getId());

            List<PublicParkListDTO> allParks = parkActivities.stream()
                .map(ParkActivity::getPark)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> PublicParkListDTO.builder()
                    .id(idObfuscator.encodeId(p.getId()))
                    .slug(p.getSlug())
                    .name(p.getName())
                    .parkType(p.getParkType())
                    .region(p.getRegion())
                    .shortDescription(p.getShortDescription())
                    .primaryImageUrl(imageResolver.resolveParkImage(p.getId(), p.getPrimaryImage()))
                    .build())
                .collect(Collectors.toList());

            int totalItems = allParks.size();
            int fromIndex = Math.min(page * size, totalItems);
            int toIndex = Math.min(fromIndex + size, totalItems);
            List<PublicParkListDTO> pagedParks = allParks.subList(fromIndex, toIndex);

            publicTranslationService.translateDtoList(pagedParks, lang);

            Map<String, Object> response = new HashMap<>();
            response.put("parks", pagedParks);
            response.put("currentPage", page);
            response.put("totalItems", (long) totalItems);
            response.put("totalPages", (int) Math.ceil((double) totalItems / size));
            response.put("pageSize", size);

            return ResponseEntity.ok(ApiResponse.success(200, "Activity parks retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching activity parks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch activity parks", "ACTIVITY_PARKS_FETCH_FAILED"));
        }
    }

    private ResponseEntity<ApiResponse<?>> buildActivityDetailResponse(Activity activity, String lang) {
        PublicActivityDetailDTO dto = convertToDetailDTO(activity);

        Pageable firstPage = PageRequest.of(0, 6);
        Page<ActivityImage> imagePage = activityImageRepository.findActiveByActivityIdPaginated(activity.getId(), firstPage);
        List<PublicImageDTO> images = imagePage.getContent().stream()
            .map(imageResolver::toPublicDTO)
            .collect(Collectors.toList());

        long totalImages = imagePage.getTotalElements();

        publicTranslationService.translateDto(dto, lang);

        Map<String, Object> response = new HashMap<>();
        response.put("activity", dto);
        response.put("images", images);
        response.put("totalImages", totalImages);
        return ResponseEntity.ok(ApiResponse.success(200, "Activity retrieved", response));
    }

    private PublicActivityListDTO convertToListDTO(Activity a) {
        return PublicActivityListDTO.builder()
            .id(idObfuscator.encodeId(a.getId()))
            .slug(a.getSlug())
            .name(a.getName())
            .description(a.getDescription())
            .primaryImageUrl(imageResolver.resolveActivityImage(a.getId(), a.getPrimaryImage()))
            .seasonAvailability(a.getSeasonAvailability())
            .build();
    }

    private PublicActivityDetailDTO convertToDetailDTO(Activity a) {
        return PublicActivityDetailDTO.builder()
            .id(idObfuscator.encodeId(a.getId()))
            .slug(a.getSlug())
            .name(a.getName())
            .description(a.getDescription())
            .detailedDescription(a.getDetailedDescription())
            .minimumAge(a.getMinimumAge())
            .maximumParticipants(a.getMaximumParticipants())
            .equipmentRequired(a.getEquipmentRequired())
            .seasonAvailability(a.getSeasonAvailability())
            .safetyInformation(a.getSafetyInformation())
            .primaryImageUrl(imageResolver.resolveActivityImage(a.getId(), a.getPrimaryImage()))
            .tags(a.getTags())
            .build();
    }
}
