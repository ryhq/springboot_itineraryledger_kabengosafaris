package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicGalleryImageDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates images from parks, activities, and accommodations
 * into a unified gallery for the public website.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PublicGalleryService {

    private final ParkImageRepository parkImageRepository;
    private final ActivityImageRepository activityImageRepository;
    private final AccommodationImageRepository accommodationImageRepository;

    private final PublicTranslationService publicTranslationService;

    @Value("${app.base.url}")
    private String appBaseUrl;

    /**
     * Get unified gallery images from all entity types.
     *
     * @param entityType Optional filter: PARK, ACTIVITY, ACCOMMODATION (null = all)
     * @param page       Page number (default 0)
     * @param size       Page size (default 24)
     * @param lang       Language for translations
     */
    public ResponseEntity<ApiResponse<?>> getGalleryImages(String entityType, Integer page, Integer size, String lang) {
        try {
            page = page != null ? page : 0;
            size = size != null ? Math.min(size, 60) : 24;

            List<PublicGalleryImageDTO> allImages = new ArrayList<>();
            long totalParkImages = 0;
            long totalActivityImages = 0;
            long totalAccommodationImages = 0;

            boolean fetchParks = entityType == null || "PARK".equalsIgnoreCase(entityType);
            boolean fetchActivities = entityType == null || "ACTIVITY".equalsIgnoreCase(entityType);
            boolean fetchAccommodations = entityType == null || "ACCOMMODATION".equalsIgnoreCase(entityType);

            if (entityType != null) {
                // Single type — use direct pagination
                Pageable pageable = PageRequest.of(page, size);

                if (fetchParks) {
                    Page<ParkImage> parkPage = parkImageRepository.findAllActiveForGallery(pageable);
                    allImages.addAll(parkPage.getContent().stream().map(this::toParkGalleryDTO).collect(Collectors.toList()));
                    totalParkImages = parkPage.getTotalElements();
                }
                if (fetchActivities) {
                    Page<ActivityImage> activityPage = activityImageRepository.findAllActiveForGallery(pageable);
                    allImages.addAll(activityPage.getContent().stream().map(this::toActivityGalleryDTO).collect(Collectors.toList()));
                    totalActivityImages = activityPage.getTotalElements();
                }
                if (fetchAccommodations) {
                    Page<AccommodationImage> accommodationPage = accommodationImageRepository.findAllActiveForGallery(pageable);
                    allImages.addAll(accommodationPage.getContent().stream().map(this::toAccommodationGalleryDTO).collect(Collectors.toList()));
                    totalAccommodationImages = accommodationPage.getTotalElements();
                }

                long totalItems = totalParkImages + totalActivityImages + totalAccommodationImages;
                int totalPages = (int) Math.ceil((double) totalItems / size);

                publicTranslationService.translateDtoList(allImages, lang);

                Map<String, Object> response = buildResponse(allImages, page, totalItems, totalPages, size,
                        totalParkImages, totalActivityImages, totalAccommodationImages);

                return ResponseEntity.ok(ApiResponse.success(200, "Gallery images retrieved", response));

            } else {
                // All types — fetch proportionally from each type
                // Get total counts first
                Pageable countPage = PageRequest.of(0, 1);
                totalParkImages = parkImageRepository.findAllActiveForGallery(countPage).getTotalElements();
                totalActivityImages = activityImageRepository.findAllActiveForGallery(countPage).getTotalElements();
                totalAccommodationImages = accommodationImageRepository.findAllActiveForGallery(countPage).getTotalElements();

                long totalItems = totalParkImages + totalActivityImages + totalAccommodationImages;

                if (totalItems == 0) {
                    Map<String, Object> response = buildResponse(Collections.emptyList(), 0, 0, 0, size, 0, 0, 0);
                    return ResponseEntity.ok(ApiResponse.success(200, "Gallery images retrieved", response));
                }

                int totalPages = (int) Math.ceil((double) totalItems / size);

                // Calculate proportional sizes
                int parkSize = (int) Math.round((double) totalParkImages / totalItems * size);
                int activitySize = (int) Math.round((double) totalActivityImages / totalItems * size);
                int accommodationSize = size - parkSize - activitySize;

                // Ensure at least 1 from each type that has images
                if (totalParkImages > 0 && parkSize == 0) parkSize = 1;
                if (totalActivityImages > 0 && activitySize == 0) activitySize = 1;
                if (totalAccommodationImages > 0 && accommodationSize == 0) accommodationSize = 1;

                // Fetch from each type
                if (totalParkImages > 0) {
                    Page<ParkImage> parkPage = parkImageRepository.findAllActiveForGallery(PageRequest.of(page, Math.max(parkSize, 1)));
                    allImages.addAll(parkPage.getContent().stream().map(this::toParkGalleryDTO).collect(Collectors.toList()));
                }
                if (totalActivityImages > 0) {
                    Page<ActivityImage> activityPage = activityImageRepository.findAllActiveForGallery(PageRequest.of(page, Math.max(activitySize, 1)));
                    allImages.addAll(activityPage.getContent().stream().map(this::toActivityGalleryDTO).collect(Collectors.toList()));
                }
                if (totalAccommodationImages > 0) {
                    Page<AccommodationImage> accommodationPage = accommodationImageRepository.findAllActiveForGallery(PageRequest.of(page, Math.max(accommodationSize, 1)));
                    allImages.addAll(accommodationPage.getContent().stream().map(this::toAccommodationGalleryDTO).collect(Collectors.toList()));
                }

                // Shuffle for visual variety
                Collections.shuffle(allImages);

                publicTranslationService.translateDtoList(allImages, lang);

                Map<String, Object> response = buildResponse(allImages, page, totalItems, totalPages, size,
                        totalParkImages, totalActivityImages, totalAccommodationImages);

                return ResponseEntity.ok(ApiResponse.success(200, "Gallery images retrieved", response));
            }

        } catch (Exception e) {
            log.error("Error fetching gallery images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch gallery images", "GALLERY_FETCH_FAILED"));
        }
    }

    // ── DTO Converters ─────────────────────────────────────────────

    private PublicGalleryImageDTO toParkGalleryDTO(ParkImage image) {
        return PublicGalleryImageDTO.builder()
                .imageUrl(toFullImageUrl("/api/park-images/file/" + image.getFileName()))
                .altText(image.getAltText())
                .caption(image.getCaption())
                .imageType(image.getImageType() != null ? image.getImageType().getDisplayName() : null)
                .entityType("PARK")
                .entityName(image.getPark().getName())
                .entitySlug(image.getPark().getSlug())
                .build();
    }

    private PublicGalleryImageDTO toActivityGalleryDTO(ActivityImage image) {
        return PublicGalleryImageDTO.builder()
                .imageUrl(toFullImageUrl("/api/activity-images/file/" + image.getFileName()))
                .altText(image.getAltText())
                .caption(image.getCaption())
                .imageType(image.getImageType() != null ? image.getImageType().getDisplayName() : null)
                .entityType("ACTIVITY")
                .entityName(image.getActivity().getName())
                .entitySlug(image.getActivity().getSlug())
                .build();
    }

    private PublicGalleryImageDTO toAccommodationGalleryDTO(AccommodationImage image) {
        return PublicGalleryImageDTO.builder()
                .imageUrl(toFullImageUrl("/api/accommodation-images/file/" + image.getFileName()))
                .altText(image.getAltText())
                .caption(image.getCaption())
                .imageType(image.getImageType() != null ? image.getImageType().getDisplayName() : null)
                .entityType("ACCOMMODATION")
                .entityName(image.getAccommodation().getName())
                .entitySlug(image.getAccommodation().getSlug())
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private String toFullImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) return relativePath;
        return appBaseUrl + (relativePath.startsWith("/") ? "" : "/") + relativePath;
    }

    private Map<String, Object> buildResponse(List<PublicGalleryImageDTO> images, int page, long totalItems,
                                               int totalPages, int size, long parkCount, long activityCount, long accommodationCount) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("images", images);
        response.put("currentPage", page);
        response.put("totalItems", totalItems);
        response.put("totalPages", totalPages);
        response.put("pageSize", size);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("parks", parkCount);
        counts.put("activities", activityCount);
        counts.put("accommodations", accommodationCount);
        counts.put("total", totalItems);
        response.put("counts", counts);

        return response;
    }
}
