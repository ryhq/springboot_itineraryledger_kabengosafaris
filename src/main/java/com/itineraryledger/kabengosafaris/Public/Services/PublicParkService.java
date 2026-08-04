package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final com.itineraryledger.kabengosafaris.ParkActivity.Repositories.ParkActivityImageRepository parkActivityImageRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final PublicEntityResolver entityResolver;
    private final PublicImageResolver imageResolver;
    private final ItineraryDayParkRepository itineraryDayParkRepository;

    private final PublicTranslationService publicTranslationService;

    public ResponseEntity<ApiResponse<?>> getParks(Integer page, Integer size, String sortBy, String sortDirection,
                                                    String region, ParkType parkType, String keyword, List<String> tags, String lang) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDirection = sortDirection != null ? sortDirection : "asc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "name";

            Specification<Park> spec = ParkSpecification.isActive(true).and(ParkSpecification.isWebActive(true));
            if (region != null && !region.isEmpty()) spec = spec.and(ParkSpecification.regionLike(region));
            if (parkType != null) spec = spec.and(ParkSpecification.hasParkType(parkType));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ParkSpecification.searchKeyword(keyword));
            if (tags != null) for (String tg : tags) if (tg != null && !tg.isBlank()) spec = spec.and(ParkSpecification.hasTag(tg));

            Page<Park> parkPage;
            if ("visited".equalsIgnoreCase(sortBy) || "safariCount".equalsIgnoreCase(sortBy)) {
                // Computed sort — order by usage count. Small dataset, so sort in memory then page.
                List<Park> all = parkRepository.findAll(spec, Sort.by("name").ascending());
                Map<Long, Long> allCounts = usageCounts(all.stream().map(Park::getId).collect(Collectors.toList()));
                all.sort((a, b) -> {
                    long ca = allCounts.getOrDefault(a.getId(), 0L), cbn = allCounts.getOrDefault(b.getId(), 0L);
                    return cbn != ca ? Long.compare(cbn, ca) : a.getName().compareToIgnoreCase(b.getName());
                });
                int from = Math.min(page * size, all.size()), to = Math.min(from + size, all.size());
                parkPage = new PageImpl<>(all.subList(from, to), PageRequest.of(page, size), all.size());
            } else {
                Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
                parkPage = parkRepository.findAll(spec, PageRequest.of(page, size, sort));
            }

            List<Park> content = parkPage.getContent();
            List<PublicParkListDTO> dtos = content.stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());

            Map<Long, Long> counts = usageCounts(content.stream().map(Park::getId).collect(Collectors.toList()));
            for (int i = 0; i < content.size(); i++) {
                dtos.get(i).setSafariCount(counts.getOrDefault(content.get(i).getId(), 0L));
            }

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

            /*
             * A park's gallery also shows the published photos of the activities it
             * offers — the park's own images lead, those follow. Merged here rather
             * than served separately so the website needs no change to benefit.
             */
            List<PublicImageDTO> pair = activityImagesForPark(park.getId());
            java.util.Map<String, Object> body = PublicServiceUtils.buildPageResponse("images", dtos, imagePage);
            if (!pair.isEmpty() && imagePage.getNumber() == 0) {
                List<PublicImageDTO> merged = new java.util.ArrayList<>(dtos);
                merged.addAll(pair);
                body.put("images", merged);
                body.put("activityImageCount", pair.size());
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Park images retrieved", body));
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

        List<PublicImageDTO> activityImages = activityImagesForPark(park.getId());
        if (!activityImages.isEmpty()) {
            images = new java.util.ArrayList<>(images);
            images.addAll(activityImages);
        }

        long totalImages = imagePage.getTotalElements() + activityImages.size();

        publicTranslationService.translateDto(dto, lang);

        Map<String, Object> response = new HashMap<>();
        response.put("park", dto);
        response.put("images", images);
        response.put("totalImages", totalImages);
        return ResponseEntity.ok(ApiResponse.success(200, "Park retrieved", response));
    }

    /**
     * Published photos of every activity this park offers, in pairing display
     * order. Each carries its activityName so a gallery can caption it.
     */
    private List<PublicImageDTO> activityImagesForPark(Long parkId) {
        return parkActivityImageRepository.findPublishedByParkId(parkId).stream()
            .map(imageResolver::toPublicDTO)
            .collect(Collectors.toList());
    }

    /** Batch: distinct active itineraries that visit each of the given parks. */
    private Map<Long, Long> usageCounts(List<Long> parkIds) {
        if (parkIds == null || parkIds.isEmpty()) return Map.of();
        return itineraryDayParkRepository.countActiveItinerariesByParkIds(parkIds).stream()
            .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()));
    }

    private PublicParkListDTO convertToListDTO(Park p) {
        return PublicParkListDTO.builder()
            .slug(p.getSlug())
            .name(p.getName())
            .parkType(p.getParkType())
            .region(p.getRegion())
            .shortDescription(p.getShortDescription())
            .primaryImageUrl(imageResolver.resolveParkImage(p.getId(), p.getPrimaryImage()))
            .tags(p.getTags())
            .build();
    }

    private PublicParkDetailDTO convertToDetailDTO(Park p) {
        return PublicParkDetailDTO.builder()
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
