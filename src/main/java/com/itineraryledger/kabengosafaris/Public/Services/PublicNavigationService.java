package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.AccommodationSpecification;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivitySpecification;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Specifications.ItinerarySpecification;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyGetService;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
public class PublicNavigationService {

    private final ParkRepository parkRepository;
    private final ActivityRepository activityRepository;
    private final AccommodationRepository accommodationRepository;
    private final ItineraryRepository itineraryRepository;
    private final TestimonyGetService testimonyGetService;
    private final IdObfuscator idObfuscator;
    private final PublicTranslationService publicTranslationService;
    private final TranslationSettingGetterServices translationSettingGetterServices;

    public ResponseEntity<ApiResponse<?>> getNavigation(String lang) {
        try {
            Map<String, Object> nav = new HashMap<>();

            // Parks - return id and name for active parks
            List<Park> activeParksList = parkRepository.findAll(
                ParkSpecification.isActive(true).and(ParkSpecification.isWebActive(true)),
                Sort.by(Sort.Direction.ASC, "name")
            );
            List<Map<String, String>> parkNavItems = activeParksList.stream().map(p -> {
                Map<String, String> item = new HashMap<>();
                item.put("id", p.getSlug() != null ? p.getSlug() : idObfuscator.encodeId(p.getId()));
                item.put("name", p.getName());
                item.put("region", p.getRegion());
                return item;
            }).collect(Collectors.toList());
            publicTranslationService.translateMapList(parkNavItems, lang, "name", "region");
            nav.put("parks", parkNavItems);

            // Activities - return id and name for active + web-active activities
            List<Activity> activeActivitiesList = activityRepository.findAll(
                ActivitySpecification.isActive(true)
                    .and(ActivitySpecification.isWebActive(true)),
                Sort.by(Sort.Direction.ASC, "name")
            );
            List<Map<String, String>> activityNavItems = activeActivitiesList.stream().map(a -> {
                Map<String, String> item = new HashMap<>();
                item.put("id", a.getSlug() != null ? a.getSlug() : idObfuscator.encodeId(a.getId()));
                item.put("name", a.getName());
                return item;
            }).collect(Collectors.toList());
            publicTranslationService.translateMapList(activityNavItems, lang, "name");
            nav.put("activities", activityNavItems);

            // Accommodations - return id, name, and region for active accommodations
            List<Accommodation> activeAccommodationsList = accommodationRepository.findAll(
                AccommodationSpecification.isActive(true).and(AccommodationSpecification.isWebActive(true)),
                Sort.by(Sort.Direction.ASC, "name")
            );
            List<Map<String, String>> accommodationNavItems = activeAccommodationsList.stream().map(a -> {
                Map<String, String> item = new HashMap<>();
                item.put("id", a.getSlug() != null ? a.getSlug() : idObfuscator.encodeId(a.getId()));
                item.put("name", a.getName());
                item.put("region", a.getRegion());
                return item;
            }).collect(Collectors.toList());
            publicTranslationService.translateMapList(accommodationNavItems, lang, "name", "region");
            nav.put("accommodations", accommodationNavItems);

            // Itineraries (as safaris for public) - return id and name for active itineraries
            List<Itinerary> activeItinerariesList = itineraryRepository.findAll(
                ItinerarySpecification.isActive(true),
                Sort.by(Sort.Direction.ASC, "name")
            );
            List<Map<String, String>> itineraryNavItems = activeItinerariesList.stream().map(i -> {
                Map<String, String> item = new HashMap<>();
                item.put("id", i.getCode() != null ? i.getCode() : idObfuscator.encodeId(i.getId()));
                item.put("name", i.getName());
                return item;
            }).collect(Collectors.toList());
            publicTranslationService.translateMapList(itineraryNavItems, lang, "name");
            nav.put("itineraries", itineraryNavItems);

            // Testimonies count (don't list individually in nav)
            nav.put("testimoniesCount", testimonyGetService.getApprovedActiveCount());

            // Translation service status
            nav.put("translationEnabled", translationSettingGetterServices.isLibreTranslateEnabled());
            nav.put("supportedLanguages", translationSettingGetterServices.getSupportedLanguages());

            return ResponseEntity.ok(ApiResponse.success(200, "Navigation data retrieved", nav));
        } catch (Exception e) {
            log.error("Error fetching navigation data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch navigation data", "NAVIGATION_FETCH_FAILED"));
        }
    }
}
