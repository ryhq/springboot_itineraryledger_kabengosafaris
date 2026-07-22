package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.ItineraryCostSummaryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Entity.ItineraryCostSummary;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Repository.ItineraryCostSummaryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Specifications.ItinerarySpecification;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicItineraryDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;

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

import java.util.ArrayList;
import java.util.Arrays;
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
    private final PublicTranslationService publicTranslationService;
    private final SafariRepository safariRepository;
    private final ItineraryDayParkRepository itineraryDayParkRepository;
    private final ItineraryDayActivityRepository itineraryDayActivityRepository;
    private final ItineraryDayAccommodationRepository itineraryDayAccommodationRepository;

    public PublicItineraryService(
            ItineraryRepository itineraryRepository,
            ItineraryCostSummaryRepository costSummaryRepository,
            PublicEntityResolver entityResolver,
            PublicImageResolver imageResolver,
            PublicTranslationService publicTranslationService,
            SafariRepository safariRepository,
            ItineraryDayParkRepository itineraryDayParkRepository,
            ItineraryDayActivityRepository itineraryDayActivityRepository,
            ItineraryDayAccommodationRepository itineraryDayAccommodationRepository) {
        this.itineraryRepository = itineraryRepository;
        this.costSummaryRepository = costSummaryRepository;
        this.entityResolver = entityResolver;
        this.imageResolver = imageResolver;
        this.publicTranslationService = publicTranslationService;
        this.safariRepository = safariRepository;
        this.itineraryDayParkRepository = itineraryDayParkRepository;
        this.itineraryDayActivityRepository = itineraryDayActivityRepository;
        this.itineraryDayAccommodationRepository = itineraryDayAccommodationRepository;
    }

    public ResponseEntity<ApiResponse<?>> getItineraries(Integer page, Integer size, String sortBy, String sortDirection,
                                                          TripType tripType, BudgetCategory budgetCategory, String keyword,
                                                          Integer minDays, Integer maxDays, String lang) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

            // Whitelist sort keys → safe entity properties (prevents 500s on bad input).
            String sortField = switch (sortBy == null ? "" : sortBy) {
                case "duration", "totalDays" -> "totalDays";
                case "name" -> "name";
                default -> "createdAt"; // newest / createdAt / popular / featured / price → newest
            };
            // NOTE: do NOT use Sort.Order.nullsLast() here — Hibernate throws
            // "Applying Null Precedence using Criteria Queries is not yet supported"
            // for Specification/Criteria queries, which 500s every request.
            Sort sort = Sort.by(dir, sortField);
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
     * Public safaris (itineraries) that VISIT a given park.
     */
    public ResponseEntity<ApiResponse<?>> getParkSafaris(String identifier, Integer page, Integer size, String lang) {
        try {
            Park park = entityResolver.resolvePark(identifier).orElse(null);
            if (park == null || !Boolean.TRUE.equals(park.getIsActive()) || !Boolean.TRUE.equals(park.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
            }
            return safariPageFromIds(itineraryDayParkRepository.findActiveItineraryIdsByParkId(park.getId()), page, size, lang);
        } catch (Exception e) {
            log.error("Error fetching safaris for park: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safaris", "SAFARIS_FETCH_FAILED"));
        }
    }

    /**
     * Public safaris (itineraries) that FEATURE a given activity.
     */
    public ResponseEntity<ApiResponse<?>> getActivitySafaris(String identifier, Integer page, Integer size, String lang) {
        try {
            Activity activity = entityResolver.resolveActivity(identifier).orElse(null);
            if (activity == null || !Boolean.TRUE.equals(activity.getIsActive()) || !Boolean.TRUE.equals(activity.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
            }
            return safariPageFromIds(itineraryDayActivityRepository.findActiveItineraryIdsByActivityId(activity.getId()), page, size, lang);
        } catch (Exception e) {
            log.error("Error fetching safaris for activity: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safaris", "SAFARIS_FETCH_FAILED"));
        }
    }

    /**
     * Public safaris (itineraries) that STAY at a given accommodation.
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationSafaris(String identifier, Integer page, Integer size, String lang) {
        try {
            Accommodation accommodation = entityResolver.resolveAccommodation(identifier).orElse(null);
            if (accommodation == null || !Boolean.TRUE.equals(accommodation.getIsActive()) || !Boolean.TRUE.equals(accommodation.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }
            return safariPageFromIds(itineraryDayAccommodationRepository.findActiveItineraryIdsByAccommodationId(accommodation.getId()), page, size, lang);
        } catch (Exception e) {
            log.error("Error fetching safaris for accommodation: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safaris", "SAFARIS_FETCH_FAILED"));
        }
    }

    /**
     * Build a paginated "safaris" page from a set of itinerary ids — active only,
     * newest first, in-memory paginated. Produces identical cards to /public/safaris.
     */
    private ResponseEntity<ApiResponse<?>> safariPageFromIds(List<Long> ids, Integer page, Integer size, String lang) {
        page = page != null ? page : 0;
        size = size != null ? size : 6;

        List<Itinerary> all = (ids == null || ids.isEmpty())
            ? Collections.emptyList()
            : itineraryRepository.findAllById(ids).stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()))
                .sorted(Comparator.comparing(Itinerary::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        int total = all.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<Itinerary> pageItems = all.subList(from, to);

        List<Long> pageIds = pageItems.stream().map(Itinerary::getId).collect(Collectors.toList());
        Map<Long, List<ItineraryCostSummary>> costsByItinerary = pageIds.isEmpty()
            ? Collections.emptyMap()
            : costSummaryRepository.findByItinerary_IdIn(pageIds).stream()
                .collect(Collectors.groupingBy(cs -> cs.getItinerary().getId()));

        List<PublicItineraryDTO> dtos = new ArrayList<>();
        for (Itinerary entity : pageItems) {
            PublicItineraryDTO dto = convertToListDTO(entity);
            dto.setCostSummary(mapToPublicCostSummary(costsByItinerary.getOrDefault(entity.getId(), Collections.emptyList())));
            dtos.add(dto);
        }

        publicTranslationService.translateDtoList(dtos, lang);

        Page<Itinerary> pageObj = new PageImpl<>(pageItems, PageRequest.of(page, size), total);
        return ResponseEntity.ok(ApiResponse.success(200, "Safaris retrieved", PublicServiceUtils.buildPageResponse("safaris", dtos, pageObj)));
    }

    /**
     * "Most booked" itineraries — those that have been converted into the most actual Safaris.
     * Ranked by count of linked Safari records; only publicly-visible (active) itineraries are returned.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getMostBooked(Integer size, String lang) {
        try {
            int limit = (size != null && size > 0) ? size : 6;
            // Pull a few extra ranked rows so we can drop any that are no longer public and still fill `limit`.
            List<Object[]> rows = safariRepository.findMostBookedItineraryIds(PageRequest.of(0, Math.max(limit * 3, limit)));
            if (rows.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(200, "Most booked safaris", Collections.emptyList()));
            }

            List<Long> orderedIds = new ArrayList<>();
            Map<Long, Long> countById = new java.util.LinkedHashMap<>();
            for (Object[] r : rows) {
                Long id = ((Number) r[0]).longValue();
                orderedIds.add(id);
                countById.put(id, ((Number) r[1]).longValue());
            }

            // Only publicly-visible itineraries (match the list endpoint: active).
            Map<Long, Itinerary> byId = itineraryRepository.findAllById(orderedIds).stream()
                .filter(it -> Boolean.TRUE.equals(it.getIsActive()))
                .collect(Collectors.toMap(Itinerary::getId, it -> it));

            List<Itinerary> ranked = orderedIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());

            if (ranked.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(200, "Most booked safaris", Collections.emptyList()));
            }

            List<Long> rankedIds = ranked.stream().map(Itinerary::getId).collect(Collectors.toList());
            Map<Long, List<ItineraryCostSummary>> costsByItinerary = costSummaryRepository
                .findByItinerary_IdIn(rankedIds).stream()
                .collect(Collectors.groupingBy(cs -> cs.getItinerary().getId()));

            List<PublicItineraryDTO> dtos = new ArrayList<>();
            for (Itinerary entity : ranked) {
                PublicItineraryDTO dto = convertToListDTO(entity);
                dto.setCostSummary(mapToPublicCostSummary(costsByItinerary.getOrDefault(entity.getId(), Collections.emptyList())));
                dto.setBookingCount(countById.get(entity.getId()));
                dtos.add(dto);
            }

            publicTranslationService.translateDtoList(dtos, lang);
            return ResponseEntity.ok(ApiResponse.success(200, "Most booked safaris", dtos));
        } catch (Exception e) {
            log.error("Error fetching most booked safaris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch most booked safaris", "SAFARIS_POPULAR_FAILED"));
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
            .paxBreakdown(mapToPaxBreakdown(itinerary))
            .build();
    }

    /**
     * Convert Itinerary to detail DTO (full day-by-day breakdown)
     */
    private PublicItineraryDTO convertToDetailDTO(Itinerary itinerary) {
        PublicItineraryDTO.PublicItineraryDTOBuilder builder = PublicItineraryDTO.builder()
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
            .inclusions(splitLines(itinerary.getInclusions()))
            .exclusions(splitLines(itinerary.getExclusions()))
            .startLocation(itinerary.getStartLocation())
            .endLocation(itinerary.getEndLocation())
            .carCount(itinerary.getCarCount())
            .totalPaxCount(itinerary.getTotalPaxCount())
            .totalDaysCount(itinerary.getDays() != null ? itinerary.getDays().size() : 0)
            .paxBreakdown(mapToPaxBreakdown(itinerary));

        // Build day-by-day
        if (itinerary.getDays() != null && !itinerary.getDays().isEmpty()) {
            List<ItineraryDay> sortedDays = itinerary.getDays().stream()
                .sorted(Comparator.comparingInt(ItineraryDay::getDayNumber))
                .collect(Collectors.toList());

            List<PublicItineraryDTO.PublicItineraryDayDTO> dayDTOs = new ArrayList<>();
            List<String> allDayImages = new ArrayList<>();

            // Nights per lodge across the trip (non-alternative day-accommodations).
            java.util.Map<Long, Integer> nightsByAcc = new java.util.HashMap<>();
            for (ItineraryDay d : sortedDays) {
                if (d.getAccommodations() != null) {
                    d.getAccommodations().stream()
                        .filter(a -> !Boolean.TRUE.equals(a.getIsAlternative()) && a.getAccommodation() != null)
                        .forEach(a -> nightsByAcc.merge(a.getAccommodation().getId(), 1, Integer::sum));
                }
            }

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
                                .parkSlug(dp.getPark().getSlug())
                                .parkName(dp.getPark().getName())
                                .primaryImageUrl(imageResolver.resolveParkImage(dp.getPark().getId(), dp.getPark().getPrimaryImage()))
                                .latitude(dp.getPark().getLatitude())
                                .longitude(dp.getPark().getLongitude())
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
                                .activitySlug(da.getActivity().getSlug())
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
                                .accommodationSlug(da.getAccommodation().getSlug())
                                .accommodationName(da.getAccommodation().getName())
                                .primaryImageUrl(imageResolver.resolveAccommodationImage(da.getAccommodation().getId()))
                                .board(da.getBoardType() != null ? da.getBoardType().getName() : null)
                                .roomType(da.getRoomType() != null ? da.getRoomType().getName() : null)
                                .roomStandard(da.getRoomStandard() != null ? da.getRoomStandard().getName() : null)
                                .nights(nightsByAcc.get(da.getAccommodation().getId()))
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

    /** Split a newline-separated TEXT field into a trimmed list (null if empty). */
    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) return null;
        List<String> out = Arrays.stream(text.split("\\r?\\n"))
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        return out.isEmpty() ? null : out;
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

        // 1. Prefer the itinerary's own uploaded hero image
        String hero = imageResolver.resolveItineraryHero(itinerary.getId());
        if (hero != null) return hero;

        // 2. Fall back to a deterministic per-safari pick — stable and diversified across trips
        long seed = itinerary.getId() != null ? itinerary.getId() : 0L;
        return imageResolver.resolveSafariImageDeterministic(seed, allParkIds, allActivityIds, allAccIds, allParkImages, allActivityImages);
    }

    private List<PublicItineraryDTO.PublicPaxDTO> mapToPaxBreakdown(Itinerary itinerary) {
        if (itinerary.getPaxList() == null || itinerary.getPaxList().isEmpty()) return null;
        return itinerary.getPaxList().stream()
            .map(pax -> PublicItineraryDTO.PublicPaxDTO.builder()
                .nationCategoryName(pax.getNationCategory() != null ? pax.getNationCategory().getName() : null)
                .ageCategoryName(pax.getAgeCategory() != null ? pax.getAgeCategory().getName() : null)
                .count(pax.getCount())
                .build())
            .collect(Collectors.toList());
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
