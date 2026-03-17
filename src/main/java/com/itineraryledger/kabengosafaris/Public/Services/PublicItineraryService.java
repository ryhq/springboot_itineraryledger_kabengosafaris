package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.ItineraryCostSummaryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Entity.ItineraryCostSummary;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Repository.ItineraryCostSummaryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Specifications.ItinerarySpecification;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicItineraryDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class PublicItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryCostSummaryRepository costSummaryRepository;
    private final PublicEntityResolver entityResolver;
    private final PublicImageResolver imageResolver;
    private final IdObfuscator idObfuscator;
    private final PublicTranslationService publicTranslationService;

    public PublicItineraryService(
            ItineraryRepository itineraryRepository,
            ItineraryCostSummaryRepository costSummaryRepository,
            PublicEntityResolver entityResolver,
            PublicImageResolver imageResolver,
            IdObfuscator idObfuscator,
            PublicTranslationService publicTranslationService) {
        this.itineraryRepository = itineraryRepository;
        this.costSummaryRepository = costSummaryRepository;
        this.entityResolver = entityResolver;
        this.imageResolver = imageResolver;
        this.idObfuscator = idObfuscator;
        this.publicTranslationService = publicTranslationService;
    }

    public ResponseEntity<ApiResponse<?>> getItineraries(Integer page, Integer size, String sortBy, String sortDirection,
                                                          TripType tripType, BudgetCategory budgetCategory, String keyword,
                                                          Integer minDays, Integer maxDays, String lang) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDirection = sortDirection != null ? sortDirection : "desc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt";

            Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Itinerary> spec = ItinerarySpecification.isActive(true);
            if (tripType != null) spec = spec.and(ItinerarySpecification.hasTripType(tripType));
            if (budgetCategory != null) spec = spec.and(ItinerarySpecification.hasBudgetCategory(budgetCategory));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ItinerarySpecification.searchKeyword(keyword));
            if (minDays != null) spec = spec.and(ItinerarySpecification.minTotalDays(minDays));
            if (maxDays != null) spec = spec.and(ItinerarySpecification.maxTotalDays(maxDays));

            Page<Itinerary> itineraryPage = itineraryRepository.findAll(spec, pageable);

            // Batch-fetch cost summaries
            List<Long> itineraryIds = itineraryPage.getContent().stream()
                .map(Itinerary::getId)
                .collect(Collectors.toList());
            Map<Long, List<ItineraryCostSummary>> costsByItinerary = costSummaryRepository
                .findByItinerary_IdIn(itineraryIds).stream()
                .collect(Collectors.groupingBy(cs -> cs.getItinerary().getId()));

            List<PublicItineraryDTO> dtos = new ArrayList<>();
            for (Itinerary entity : itineraryPage.getContent()) {
                PublicItineraryDTO dto = convertToListDTO(entity);
                List<ItineraryCostSummary> costs = costsByItinerary.getOrDefault(entity.getId(), Collections.emptyList());
                dto.setCostSummary(mapToPublicCostSummary(costs));
                dtos.add(dto);
            }

            publicTranslationService.translateDtoList(dtos, lang);

            return ResponseEntity.ok(ApiResponse.success(200, "Safaris retrieved", PublicServiceUtils.buildPageResponse("safaris", dtos, itineraryPage)));
        } catch (Exception e) {
            log.error("Error fetching public itineraries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safaris", "SAFARIS_FETCH_FAILED"));
        }
    }

    /**
     * Get itinerary by identifier (obfuscated id or code)
     */
    public ResponseEntity<ApiResponse<?>> getItineraryByIdentifier(String identifier, String lang) {
        try {
            Itinerary itinerary = entityResolver.resolveItinerary(identifier).orElse(null);
            if (itinerary == null || !Boolean.TRUE.equals(itinerary.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND"));
            }

            PublicItineraryDTO dto = convertToDetailDTO(itinerary);

            List<ItineraryCostSummary> costs = costSummaryRepository.findByItineraryId(itinerary.getId());
            dto.setCostSummary(mapToPublicCostSummary(costs));

            publicTranslationService.translateDto(dto, lang);

            return ResponseEntity.ok(ApiResponse.success(200, "Safari retrieved", dto));
        } catch (Exception e) {
            log.error("Error fetching itinerary by identifier: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safari", "SAFARI_FETCH_FAILED"));
        }
    }

    /**
     * Convert Itinerary to list DTO (lightweight, with primary image)
     */
    private PublicItineraryDTO convertToListDTO(Itinerary itinerary) {
        // Resolve primary image: entity field → random from days
        String primaryImage = itinerary.getPrimaryImageUrl();
        if (primaryImage == null && itinerary.getDays() != null) {
            primaryImage = pickRandomImageFromItinerary(itinerary);
        }

        return PublicItineraryDTO.builder()
            .id(idObfuscator.encodeId(itinerary.getId()))
            .name(itinerary.getName())
            .code(itinerary.getCode())
            .tripType(itinerary.getTripType())
            .tripTypeDisplayName(itinerary.getTripType() != null ? itinerary.getTripType().getDisplayName() : null)
            .budgetCategory(itinerary.getBudgetCategory())
            .budgetCategoryDisplayName(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDisplayName() : null)
            .budgetCategoryTier(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getTier() : null)
            .totalDays(itinerary.getTotalDays())
            .totalNights(itinerary.getTotalNights())
            .isDayTrip(itinerary.getTotalDays() != null && itinerary.getTotalDays() == 1
                && itinerary.getTotalNights() != null && itinerary.getTotalNights() == 0)
            .description(itinerary.getDescription())
            .highlights(itinerary.getHighlights())
            .startLocation(itinerary.getStartLocation())
            .endLocation(itinerary.getEndLocation())
            .totalPaxCount(itinerary.getTotalPaxCount())
            .totalDaysCount(itinerary.getDays() != null ? itinerary.getDays().size() : 0)
            .primaryImageUrl(primaryImage)
            .build();
    }

    /**
     * Convert Itinerary to detail DTO (full day-by-day breakdown)
     */
    private PublicItineraryDTO convertToDetailDTO(Itinerary itinerary) {
        PublicItineraryDTO.PublicItineraryDTOBuilder builder = PublicItineraryDTO.builder()
            .id(idObfuscator.encodeId(itinerary.getId()))
            .name(itinerary.getName())
            .code(itinerary.getCode())
            .status(itinerary.getStatus())
            .statusDisplayName(itinerary.getStatus() != null ? itinerary.getStatus().getDisplayName() : null)
            .tripType(itinerary.getTripType())
            .tripTypeDisplayName(itinerary.getTripType() != null ? itinerary.getTripType().getDisplayName() : null)
            .tripTypeDescription(itinerary.getTripType() != null ? itinerary.getTripType().getDescription() : null)
            .budgetCategory(itinerary.getBudgetCategory())
            .budgetCategoryDisplayName(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDisplayName() : null)
            .budgetCategoryDescription(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDescription() : null)
            .budgetCategoryTier(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getTier() : null)
            .totalDays(itinerary.getTotalDays())
            .totalNights(itinerary.getTotalNights())
            .isDayTrip(itinerary.getTotalDays() != null && itinerary.getTotalDays() == 1
                && itinerary.getTotalNights() != null && itinerary.getTotalNights() == 0)
            .description(itinerary.getDescription())
            .highlights(itinerary.getHighlights())
            .startLocation(itinerary.getStartLocation())
            .endLocation(itinerary.getEndLocation())
            .carCount(itinerary.getCarCount())
            .totalPaxCount(itinerary.getTotalPaxCount())
            .totalDaysCount(itinerary.getDays() != null ? itinerary.getDays().size() : 0);

        // Build day-by-day
        if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
            List<ItineraryDay> sortedDays = itinerary.getDays().stream()
                .sorted(Comparator.comparingInt(ItineraryDay::getDayNumber))
                .collect(Collectors.toList());

            List<PublicItineraryDTO.PublicItineraryDayDTO> dayDTOs = new ArrayList<>();
            List<String> allDayImages = new ArrayList<>();

            for (ItineraryDay day : sortedDays) {
                List<Long> dayParkIds = new ArrayList<>();
                List<Long> dayActivityIds = new ArrayList<>();
                List<Long> dayAccommodationIds = new ArrayList<>();
                List<String> dayParkEntityImages = new ArrayList<>();
                List<String> dayActivityEntityImages = new ArrayList<>();

                PublicItineraryDTO.PublicItineraryDayDTO.PublicItineraryDayDTOBuilder dayBuilder =
                    PublicItineraryDTO.PublicItineraryDayDTO.builder()
                        .dayNumber(day.getDayNumber())
                        .dayTag(day.getDayTag())
                        .title(day.getTitle())
                        .description(day.getDescription())
                        .morningActivities(day.getMorningActivities())
                        .afternoonActivities(day.getAfternoonActivities())
                        .eveningActivities(day.getEveningActivities())
                        .wildlifeHighlights(day.getWildlifeHighlights())
                        .scenicHighlights(day.getScenicHighlights())
                        .specialNotes(day.getSpecialNotes())
                        .startLocation(day.getStartLocation())
                        .endLocation(day.getEndLocation())
                        .distanceKm(day.getDistanceKm())
                        .isOvernight(day.getIsOvernight())
                        .mealsIncluded(day.getMealsIncluded());

                // Parks
                if (day.getParks() != null && !day.getParks().isEmpty()) {
                    List<PublicItineraryDTO.DayParkDTO> parkDTOs = day.getParks().stream()
                        .map(dp -> {
                            dayParkIds.add(dp.getPark().getId());
                            dayParkEntityImages.add(dp.getPark().getPrimaryImage());
                            return PublicItineraryDTO.DayParkDTO.builder()
                                .parkId(idObfuscator.encodeId(dp.getPark().getId()))
                                .parkName(dp.getPark().getName())
                                .primaryImageUrl(imageResolver.resolveParkImage(dp.getPark().getId(), dp.getPark().getPrimaryImage()))
                                .build();
                        })
                        .collect(Collectors.toList());
                    dayBuilder.parks(parkDTOs);
                }

                // Activities
                if (day.getActivities() != null && !day.getActivities().isEmpty()) {
                    List<PublicItineraryDTO.DayActivityDTO> activityDTOs = day.getActivities().stream()
                        .map(da -> {
                            dayActivityIds.add(da.getActivity().getId());
                            dayActivityEntityImages.add(da.getActivity().getPrimaryImage());
                            return PublicItineraryDTO.DayActivityDTO.builder()
                                .activityId(idObfuscator.encodeId(da.getActivity().getId()))
                                .activityName(da.getActivity().getName())
                                .durationHours(da.getDurationHours())
                                .isOptional(da.getIsOptional())
                                .build();
                        })
                        .collect(Collectors.toList());
                    dayBuilder.activities(activityDTOs);
                }

                // Accommodations
                if (day.getAccommodations() != null && !day.getAccommodations().isEmpty()) {
                    List<PublicItineraryDTO.DayAccommodationDTO> accDTOs = day.getAccommodations().stream()
                        .filter(da -> !Boolean.TRUE.equals(da.getIsAlternative()))
                        .map(da -> {
                            dayAccommodationIds.add(da.getAccommodation().getId());
                            return PublicItineraryDTO.DayAccommodationDTO.builder()
                                .accommodationId(idObfuscator.encodeId(da.getAccommodation().getId()))
                                .accommodationName(da.getAccommodation().getName())
                                .primaryImageUrl(imageResolver.resolveAccommodationImage(da.getAccommodation().getId()))
                                .build();
                        })
                        .collect(Collectors.toList());
                    dayBuilder.accommodations(accDTOs);
                }

                // Day image: use entity's primaryImageUrl or pick from collected images
                String dayImage = day.getPrimaryImageUrl();
                if (dayImage == null) {
                    List<String> dayImagePool = imageResolver.collectDayImages(
                        dayParkIds, dayActivityIds, dayAccommodationIds,
                        dayParkEntityImages, dayActivityEntityImages);
                    dayImage = imageResolver.pickRandom(dayImagePool);
                    allDayImages.addAll(dayImagePool);
                } else {
                    allDayImages.add(dayImage);
                }
                dayBuilder.dayImageUrl(dayImage);

                dayDTOs.add(dayBuilder.build());
            }

            builder.days(dayDTOs);

            // Primary image: entity field → random from all day images
            String primaryImage = itinerary.getPrimaryImageUrl();
            if (primaryImage == null && !allDayImages.isEmpty()) {
                List<String> unique = allDayImages.stream().distinct().collect(Collectors.toList());
                primaryImage = imageResolver.pickRandom(unique);
            }
            builder.primaryImageUrl(primaryImage);
        }

        return builder.build();
    }

    /**
     * Pick a random image from an itinerary's days for list views.
     */
    private String pickRandomImageFromItinerary(Itinerary itinerary) {
        if (itinerary.getDays() == null) return null;

        List<Long> allParkIds = new ArrayList<>();
        List<Long> allActivityIds = new ArrayList<>();
        List<Long> allAccIds = new ArrayList<>();
        List<String> allParkImages = new ArrayList<>();
        List<String> allActivityImages = new ArrayList<>();

        for (ItineraryDay day : itinerary.getDays()) {
            if (day.getParks() != null) {
                day.getParks().forEach(dp -> {
                    allParkIds.add(dp.getPark().getId());
                    allParkImages.add(dp.getPark().getPrimaryImage());
                });
            }
            if (day.getActivities() != null) {
                day.getActivities().forEach(da -> {
                    allActivityIds.add(da.getActivity().getId());
                    allActivityImages.add(da.getActivity().getPrimaryImage());
                });
            }
            if (day.getAccommodations() != null) {
                day.getAccommodations().stream()
                    .filter(da -> !Boolean.TRUE.equals(da.getIsAlternative()))
                    .forEach(da -> allAccIds.add(da.getAccommodation().getId()));
            }
        }

        // Try priority: parks > activities > accommodations
        String resolved = imageResolver.resolveSafariImage(allParkIds, allActivityIds, allAccIds, allParkImages, allActivityImages);
        if (resolved != null) return resolved;

        // Fallback: collect all and pick random
        List<String> pool = imageResolver.collectDayImages(allParkIds, allActivityIds, allAccIds, allParkImages, allActivityImages);
        return imageResolver.pickRandom(pool);
    }

    private List<ItineraryCostSummaryDTO> mapToPublicCostSummary(List<ItineraryCostSummary> costs) {
        if (costs == null || costs.isEmpty()) return null;
        return costs.stream()
            .map(cs -> ItineraryCostSummaryDTO.builder()
                .currency(cs.getCurrency())
                .accommodationRack(cs.getAccommodationRack())
                .parkFeesRack(cs.getParkFeesRack())
                .activitiesRack(cs.getActivitiesRack())
                .grandTotalRack(cs.getGrandTotalRack())
                .build())
            .collect(Collectors.toList());
    }
}
