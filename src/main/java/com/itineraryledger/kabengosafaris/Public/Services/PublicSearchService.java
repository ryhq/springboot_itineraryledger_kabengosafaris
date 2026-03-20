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
import com.itineraryledger.kabengosafaris.Public.DTOs.*;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
import com.itineraryledger.kabengosafaris.Testimony.Specifications.TestimonySpecification;
import jakarta.persistence.criteria.Predicate;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class PublicSearchService {

    private final ParkRepository parkRepository;
    private final ActivityRepository activityRepository;
    private final AccommodationRepository accommodationRepository;
    private final ItineraryRepository itineraryRepository;
    private final TestimonyRepository testimonyRepository;

    private final PublicImageResolver imageResolver;
    private final PublicTestimonyService publicTestimonyService;
    private final PublicTranslationService publicTranslationService;
    private final TranslationService translationService;

    private static final int PREVIEW_LIMIT = 3;

    /**
     * Safe search specs that only use VARCHAR fields (not TEXT/CLOB) to avoid
     * Hibernate lower() type mismatch errors.
     */
    private static <T> Specification<T> safeSearch(String keyword, String... fields) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            Predicate[] predicates = new Predicate[fields.length];
            for (int i = 0; i < fields.length; i++) {
                predicates[i] = cb.like(cb.lower(root.get(fields[i])), pattern);
            }
            return cb.or(predicates);
        };
    }

    public ResponseEntity<ApiResponse<?>> search(String keyword, String lang) {
        return search(keyword, lang, PREVIEW_LIMIT);
    }

    public ResponseEntity<ApiResponse<?>> search(String keyword, String lang, int limit) {
        log.info("Global search for keyword: '{}', lang: {}, limit: {}", keyword, lang, limit);

        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "Search keyword must be at least 2 characters", "KEYWORD_TOO_SHORT"));
        }

        try {
            // If user is searching in a non-English language, translate the keyword to English
            // so we can match against the English source data in the database.
            // Search with BOTH the original keyword and the translated keyword for best results.
            String searchKeyword = keyword.trim();
            if (lang != null && !lang.isBlank() && !"en".equalsIgnoreCase(lang) && translationService.isAvailable()) {
                try {
                    String translated = translationService.translatePlainText(searchKeyword, lang, "en");
                    if (translated != null && !translated.isBlank() && !translated.equalsIgnoreCase(searchKeyword)) {
                        searchKeyword = translated;
                        log.info("Translated search keyword '{}' ({}) -> '{}' (en)", keyword.trim(), lang, searchKeyword);
                    }
                } catch (Exception e) {
                    log.debug("Keyword translation failed, searching with original keyword: {}", e.getMessage());
                }
            }

            Map<String, Object> results = new HashMap<>();

            // Parks — safe VARCHAR fields only (location, shortDescription, fullDescription are TEXT)
            Specification<Park> parkSpec = ParkSpecification.isActive(true)
                .and(ParkSpecification.isWebActive(true))
                .and(safeSearch(searchKeyword, "name", "region", "district"));
            Page<Park> parkPage = parkRepository.findAll(parkSpec,
                PageRequest.of(0, limit, Sort.by("name").ascending()));
            List<PublicParkListDTO> parkDtos = parkPage.getContent().stream()
                .<PublicParkListDTO>map(p -> PublicParkListDTO.builder()
                    .slug(p.getSlug())
                    .name(p.getName())
                    .parkType(p.getParkType())
                    .region(p.getRegion())
                    .shortDescription(p.getShortDescription())
                    .primaryImageUrl(imageResolver.resolveParkImage(p.getId(), p.getPrimaryImage()))
                    .build())
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(parkDtos, lang);
            results.put("parks", parkDtos);
            results.put("parksTotalItems", parkPage.getTotalElements());

            // Activities — safe VARCHAR fields only (description, tags, equipmentRequired, safetyInformation are TEXT)
            Specification<Activity> activitySpec = ActivitySpecification.isActive(true)
                .and(ActivitySpecification.isWebActive(true))
                .and(safeSearch(searchKeyword, "name", "seasonAvailability"));
            Page<Activity> activityPage = activityRepository.findAll(activitySpec,
                PageRequest.of(0, limit, Sort.by("name").ascending()));
            List<PublicActivityListDTO> activityDtos = activityPage.getContent().stream()
                .<PublicActivityListDTO>map(a -> PublicActivityListDTO.builder()
                    .slug(a.getSlug())
                    .name(a.getName())
                    .description(a.getDescription())
                    .primaryImageUrl(imageResolver.resolveActivityImage(a.getId(), a.getPrimaryImage()))
                    .seasonAvailability(a.getSeasonAvailability())
                    .build())
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(activityDtos, lang);
            results.put("activities", activityDtos);
            results.put("activitiesTotalItems", activityPage.getTotalElements());

            // Accommodations — already safe (excludes @Lob fields)
            Specification<Accommodation> accSpec = AccommodationSpecification.isActive(true)
                .and(AccommodationSpecification.isWebActive(true))
                .and(AccommodationSpecification.searchKeyword(searchKeyword));
            Page<Accommodation> accPage = accommodationRepository.findAll(accSpec,
                PageRequest.of(0, limit, Sort.by("name").ascending()));
            List<PublicAccommodationListDTO> accDtos = accPage.getContent().stream()
                .<PublicAccommodationListDTO>map(a -> PublicAccommodationListDTO.builder()
                    .slug(a.getSlug())
                    .name(a.getName())
                    .accommodationType(a.getAccommodationType())
                    .accommodationTypeDisplayName(a.getAccommodationType() != null ? a.getAccommodationType().getDisplayName() : null)
                    .category(a.getCategory())
                    .categoryDisplayName(a.getCategory() != null ? a.getCategory().getDisplayName() : null)
                    .starRating(a.getStarRating())
                    .region(a.getRegion())
                    .shortDescription(a.getShortDescription())
                    .primaryImageUrl(imageResolver.resolveAccommodationImage(a.getId()))
                    .build())
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(accDtos, lang);
            results.put("accommodations", accDtos);
            results.put("accommodationsTotalItems", accPage.getTotalElements());

            // Safaris (Itineraries) — safe VARCHAR fields only (description, highlights are TEXT)
            Specification<Itinerary> itinSpec = ItinerarySpecification.isActive(true)
                .and(safeSearch(searchKeyword, "name", "code", "startLocation", "endLocation"));
            Page<Itinerary> itinPage = itineraryRepository.findAll(itinSpec,
                PageRequest.of(0, limit, Sort.by("name").ascending()));
            List<PublicItineraryDTO> safariDtos = itinPage.getContent().stream()
                .<PublicItineraryDTO>map(i -> PublicItineraryDTO.builder()
                    .name(i.getName())
                    .code(i.getCode())
                    .tripType(i.getTripType())
                    .tripTypeDisplayName(i.getTripType() != null ? i.getTripType().getDisplayName() : null)
                    .budgetCategory(i.getBudgetCategory())
                    .budgetCategoryDisplayName(i.getBudgetCategory() != null ? i.getBudgetCategory().getDisplayName() : null)
                    .totalDays(i.getTotalDays())
                    .totalNights(i.getTotalNights())
                    .description(i.getDescription())
                    .primaryImageUrl(i.getPrimaryImageUrl())
                    .build())
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(safariDtos, lang);
            results.put("safaris", safariDtos);
            results.put("safarisTotalItems", itinPage.getTotalElements());

            // Testimonials — safe VARCHAR fields only (message is TEXT)
            Specification<Testimony> testSpec = TestimonySpecification.isApproved(true)
                .and(TestimonySpecification.isActive(true))
                .and(safeSearch(searchKeyword, "authorName"));
            Page<Testimony> testPage = testimonyRepository.findAll(testSpec,
                PageRequest.of(0, limit, Sort.by("displayOrder").ascending()));
            List<PublicTestimonyDTO> testDtos = testPage.getContent().stream()
                .<PublicTestimonyDTO>map(publicTestimonyService::convertToPublicDTO)
                .collect(Collectors.toList());
            publicTranslationService.translateDtoList(testDtos, lang);
            results.put("testimonies", testDtos);
            results.put("testimoniesTotalItems", testPage.getTotalElements());

            long totalResults = parkPage.getTotalElements() + activityPage.getTotalElements()
                + accPage.getTotalElements() + itinPage.getTotalElements() + testPage.getTotalElements();
            results.put("totalResults", totalResults);

            return ResponseEntity.ok(ApiResponse.success(200, "Search results retrieved successfully", results));
        } catch (Exception e) {
            log.error("Error performing global search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to perform search", "SEARCH_FAILED"));
        }
    }
}
