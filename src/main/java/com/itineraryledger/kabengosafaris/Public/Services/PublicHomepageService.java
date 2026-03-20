package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivitySpecification;
import com.itineraryledger.kabengosafaris.Hero.Entity.Hero;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Hero.Repository.HeroRepository;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicHeroDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Specifications.ItinerarySpecification;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicActivityListDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicItineraryDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicParkListDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicTestimonyDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicHomepageService {

    private final HeroRepository heroRepository;
    private final PublicHeroService publicHeroService;
    private final ItineraryRepository itineraryRepository;

    private final PublicImageResolver imageResolver;
    private final ParkRepository parkRepository;
    private final TestimonyRepository testimonyRepository;
    private final PublicTestimonyService publicTestimonyService;
    private final ActivityRepository activityRepository;
    private final PublicTranslationService publicTranslationService;

    private static final int DEFAULT_SAFARIS_LIMIT = 6;
    private static final int DEFAULT_PARKS_LIMIT = 6;
    private static final int DEFAULT_ACTIVITIES_LIMIT = 6;
    private static final int DEFAULT_TESTIMONIES_LIMIT = 3;

    /**
     * Aggregated homepage data: heroes, safaris (itineraries), parks, activities, and featured testimonies
     * in a single request to reduce client-side round trips.
     */
    public ResponseEntity<ApiResponse<?>> getHomepageData(String lang) {
        try {
            Map<String, Object> homepage = new HashMap<>();

            // Heroes for HOME page
            List<Hero> heroes = heroRepository.findByPageAndIsActiveTrueOrderByDisplayOrderAsc(HeroPage.HOME);
            List<PublicHeroDTO> heroDtos = heroes.stream()
                .map(publicHeroService::convertToPublicDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(heroDtos, lang);
            homepage.put("heroes", heroDtos);

            // Safaris (Itineraries exposed as Safaris - first page, limited)
            Page<Itinerary> itineraryPage = itineraryRepository.findAll(
                ItinerarySpecification.isActive(true),
                PageRequest.of(0, DEFAULT_SAFARIS_LIMIT, Sort.by("createdAt").descending())
            );
            List<PublicItineraryDTO> safariDtos = itineraryPage.getContent().stream()
                .map(this::convertToListItineraryDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(safariDtos, lang);
            homepage.put("safaris", safariDtos);
            homepage.put("safarisTotalItems", itineraryPage.getTotalElements());

            // Parks (first page, limited)
            Page<Park> parkPage = parkRepository.findAll(
                ParkSpecification.isActive(true).and(ParkSpecification.isWebActive(true)),
                PageRequest.of(0, DEFAULT_PARKS_LIMIT, Sort.by("name").ascending())
            );
            List<PublicParkListDTO> parkDtos = parkPage.getContent().stream()
                .map(this::convertToParkListDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(parkDtos, lang);
            homepage.put("parks", parkDtos);
            homepage.put("parksTotalItems", parkPage.getTotalElements());

            // Activities (first page, limited)
            Page<Activity> activityPage = activityRepository.findAll(
                ActivitySpecification.isActive(true).and(ActivitySpecification.isWebActive(true)),
                PageRequest.of(0, DEFAULT_ACTIVITIES_LIMIT, Sort.by("name").ascending())
            );
            List<PublicActivityListDTO> activityDtos = activityPage.getContent().stream()
                .map(this::convertToActivityListDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(activityDtos, lang);
            homepage.put("activities", activityDtos);
            homepage.put("activitiesTotalItems", activityPage.getTotalElements());

            // Featured testimonies (limited)
            List<Testimony> featuredTestimonies = testimonyRepository
                .findByIsFeaturedTrueAndIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();
            List<PublicTestimonyDTO> testimonyDtos = featuredTestimonies.stream()
                .limit(DEFAULT_TESTIMONIES_LIMIT)
                .map(publicTestimonyService::convertToPublicDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(testimonyDtos, lang);
            homepage.put("testimonies", testimonyDtos);
            homepage.put("testimoniesTotalItems", (long) featuredTestimonies.size());

            return ResponseEntity.ok(ApiResponse.success(200, "Homepage data retrieved successfully", homepage));
        } catch (Exception e) {
            log.error("Error fetching homepage data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch homepage data", "HOMEPAGE_FETCH_FAILED"));
        }
    }

    private PublicItineraryDTO convertToListItineraryDTO(Itinerary itinerary) {
        // Resolve primary image: entity field → random from linked entities
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
            .build();
    }

    private String pickRandomImageFromItinerary(Itinerary itinerary) {
        if (itinerary.getDays() == null) return null;

        List<Long> allParkIds = new ArrayList<>();
        List<Long> allActivityIds = new ArrayList<>();
        List<Long> allAccIds = new ArrayList<>();
        List<String> allParkImages = new ArrayList<>();
        List<String> allActivityImages = new ArrayList<>();

        for (var day : itinerary.getDays()) {
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

        String resolved = imageResolver.resolveSafariImage(allParkIds, allActivityIds, allAccIds, allParkImages, allActivityImages);
        if (resolved != null) return resolved;

        List<String> pool = imageResolver.collectDayImages(allParkIds, allActivityIds, allAccIds, allParkImages, allActivityImages);
        return imageResolver.pickRandom(pool);
    }

    private PublicParkListDTO convertToParkListDTO(Park p) {
        return PublicParkListDTO.builder()
            .slug(p.getSlug())
            .name(p.getName())
            .parkType(p.getParkType())
            .region(p.getRegion())
            .shortDescription(p.getShortDescription())
            .primaryImageUrl(imageResolver.resolveParkImage(p.getId(), p.getPrimaryImage()))
            .build();
    }

    private PublicActivityListDTO convertToActivityListDTO(Activity a) {
        return PublicActivityListDTO.builder()
            .slug(a.getSlug())
            .name(a.getName())
            .description(a.getDescription())
            .primaryImageUrl(imageResolver.resolveActivityImage(a.getId(), a.getPrimaryImage()))
            .seasonAvailability(a.getSeasonAvailability())
            .build();
    }
}
