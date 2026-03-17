package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicActivityListDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicImageDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicParkDetailDTO;
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
public class PublicParkService {

    private final ParkRepository parkRepository;
    private final ParkImageRepository parkImageRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final PublicEntityResolver entityResolver;
    private final PublicImageResolver imageResolver;
    private final IdObfuscator idObfuscator;
    private final PublicTranslationService publicTranslationService;

    public ResponseEntity<ApiResponse<?>> getParks(Integer page, Integer size, String sortBy, String sortDirection,
                                                    String region, ParkType parkType, String keyword, String lang) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDirection = sortDirection != null ? sortDirection : "asc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "name";

            Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Park> spec = ParkSpecification.isActive(true).and(ParkSpecification.isWebActive(true));
            if (region != null && !region.isEmpty()) spec = spec.and(ParkSpecification.regionLike(region));
            if (parkType != null) spec = spec.and(ParkSpecification.hasParkType(parkType));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ParkSpecification.searchKeyword(keyword));

            Page<Park> parkPage = parkRepository.findAll(spec, pageable);
            List<PublicParkListDTO> dtos = parkPage.getContent().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());

            publicTranslationService.translateDtoList(dtos, lang);

            return ResponseEntity.ok(ApiResponse.success(200, "Parks retrieved", PublicServiceUtils.buildPageResponse("parks", dtos, parkPage)));
        } catch (Exception e) {
            log.error("Error fetching public parks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch parks", "PARKS_FETCH_FAILED"));
        }
    }

    /**
     * Get park by identifier (obfuscated id or slug)
     */
    public ResponseEntity<ApiResponse<?>> getParkByIdentifier(String identifier, String lang) {
        try {
            Park park = entityResolver.resolvePark(identifier).orElse(null);
            if (park == null || !Boolean.TRUE.equals(park.getIsActive()) || !Boolean.TRUE.equals(park.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
            }
            return buildParkDetailResponse(park, lang);
        } catch (Exception e) {
            log.error("Error fetching park by identifier: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch park", "PARK_FETCH_FAILED"));
        }
    }

    /**
     * Get paginated images for a park (by identifier)
     */
    public ResponseEntity<ApiResponse<?>> getParkImages(String identifier, Integer page, Integer size) {
        try {
            Park park = entityResolver.resolvePark(identifier).orElse(null);
            if (park == null || !Boolean.TRUE.equals(park.getIsActive()) || !Boolean.TRUE.equals(park.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
            }

            page = page != null ? page : 0;
            size = size != null ? size : 6;
            Pageable pageable = PageRequest.of(page, size);

            Page<ParkImage> imagePage = parkImageRepository.findActiveByParkIdPaginated(park.getId(), pageable);
            List<PublicImageDTO> dtos = imagePage.getContent().stream()
                .map(imageResolver::toPublicDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Park images retrieved", PublicServiceUtils.buildPageResponse("images", dtos, imagePage)));
        } catch (Exception e) {
            log.error("Error fetching park images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch park images", "PARK_IMAGES_FETCH_FAILED"));
        }
    }

    /**
     * Get paginated activities for a park (by identifier)
     */
    public ResponseEntity<ApiResponse<?>> getParkActivities(String identifier, Integer page, Integer size, String lang) {
        try {
            Park park = entityResolver.resolvePark(identifier).orElse(null);
            if (park == null || !Boolean.TRUE.equals(park.getIsActive()) || !Boolean.TRUE.equals(park.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
            }

            page = page != null ? page : 0;
            size = size != null ? size : 6;

            List<ParkActivity> parkActivities = parkActivityRepository.findByParkIdWithActivity(park.getId());

            List<PublicActivityListDTO> allActivities = parkActivities.stream()
                .map(ParkActivity::getActivity)
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()) && Boolean.TRUE.equals(a.getIsWebActive()))
                .map(a -> PublicActivityListDTO.builder()
                    .id(idObfuscator.encodeId(a.getId()))
                    .slug(a.getSlug())
                    .name(a.getName())
                    .description(a.getDescription())
                    .primaryImageUrl(imageResolver.resolveActivityImage(a.getId(), a.getPrimaryImage()))
                    .seasonAvailability(a.getSeasonAvailability())
                    .build())
                .collect(Collectors.toList());

            int totalItems = allActivities.size();
            int fromIndex = Math.min(page * size, totalItems);
            int toIndex = Math.min(fromIndex + size, totalItems);
            List<PublicActivityListDTO> pagedActivities = allActivities.subList(fromIndex, toIndex);

            publicTranslationService.translateDtoList(pagedActivities, lang);

            Map<String, Object> response = new HashMap<>();
            response.put("activities", pagedActivities);
            response.put("currentPage", page);
            response.put("totalItems", (long) totalItems);
            response.put("totalPages", (int) Math.ceil((double) totalItems / size));
            response.put("pageSize", size);

            return ResponseEntity.ok(ApiResponse.success(200, "Park activities retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch park activities", "PARK_ACTIVITIES_FETCH_FAILED"));
        }
    }

    private ResponseEntity<ApiResponse<?>> buildParkDetailResponse(Park park, String lang) {
        PublicParkDetailDTO dto = convertToDetailDTO(park);

        Pageable firstPage = PageRequest.of(0, 6);
        Page<ParkImage> imagePage = parkImageRepository.findActiveByParkIdPaginated(park.getId(), firstPage);
        List<PublicImageDTO> images = imagePage.getContent().stream()
            .map(imageResolver::toPublicDTO)
            .collect(Collectors.toList());

        long totalImages = imagePage.getTotalElements();

        publicTranslationService.translateDto(dto, lang);

        Map<String, Object> response = new HashMap<>();
        response.put("park", dto);
        response.put("images", images);
        response.put("totalImages", totalImages);
        return ResponseEntity.ok(ApiResponse.success(200, "Park retrieved", response));
    }

    private PublicParkListDTO convertToListDTO(Park p) {
        return PublicParkListDTO.builder()
            .id(idObfuscator.encodeId(p.getId()))
            .slug(p.getSlug())
            .name(p.getName())
            .parkType(p.getParkType())
            .region(p.getRegion())
            .shortDescription(p.getShortDescription())
            .primaryImageUrl(imageResolver.resolveParkImage(p.getId(), p.getPrimaryImage()))
            .build();
    }

    private PublicParkDetailDTO convertToDetailDTO(Park p) {
        return PublicParkDetailDTO.builder()
            .id(idObfuscator.encodeId(p.getId()))
            .slug(p.getSlug())
            .name(p.getName())
            .parkType(p.getParkType())
            .region(p.getRegion())
            .district(p.getDistrict())
            .location(p.getLocation())
            .latitude(p.getLatitude())
            .longitude(p.getLongitude())
            .elevation(p.getElevation())
            .size(p.getSize())
            .shortDescription(p.getShortDescription())
            .fullDescription(p.getFullDescription())
            .history(p.getHistory())
            .ecosystem(p.getEcosystem())
            .wildlife(p.getWildlife())
            .vegetation(p.getVegetation())
            .primaryImageUrl(imageResolver.resolveParkImage(p.getId(), p.getPrimaryImage()))
            .bestTimeToVisit(p.getBestTimeToVisit())
            .openingHours(p.getOpeningHours())
            .accessInformation(p.getAccessInformation())
            .tags(p.getTags())
            .build();
    }
}
