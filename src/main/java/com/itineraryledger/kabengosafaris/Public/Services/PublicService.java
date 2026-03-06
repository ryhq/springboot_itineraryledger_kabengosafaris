package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationImageDTOs.AccommodationImageDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationImageServices.AccommodationImageGetService;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.AccommodationSpecification;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ActivitySpecification;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDTO;
import com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs.ActivityImageDTO;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityGetService;
import com.itineraryledger.kabengosafaris.Activity.Services.ActivityImageServices.ActivityImageGetService;
import com.itineraryledger.kabengosafaris.Hero.Enums.HeroPage;
import com.itineraryledger.kabengosafaris.Hero.Services.HeroServices.HeroGetService;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Services.ItineraryGetService;
import com.itineraryledger.kabengosafaris.Itinerary.Specifications.ItinerarySpecification;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkDTO;
import com.itineraryledger.kabengosafaris.Park.DTOs.ParkImageDTOs.ParkImageDTO;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkSpecification;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.Park.Services.ParkGetService;
import com.itineraryledger.kabengosafaris.Park.Services.ParkImageServices.ParkImageGetService;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicAccommodationDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicSafariDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.Specifications.SafariSpecification;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices.TestimonyGetService;
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
public class PublicService {

    private final ParkRepository parkRepository;
    private final ParkGetService parkGetService;
    private final ParkImageRepository parkImageRepository;
    private final ParkImageGetService parkImageGetService;

    private final ActivityRepository activityRepository;
    private final ActivityGetService activityGetService;
    private final ActivityImageRepository activityImageRepository;
    private final ActivityImageGetService activityImageGetService;

    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageRepository accommodationImageRepository;
    private final AccommodationImageGetService accommodationImageGetService;

    private final ItineraryRepository itineraryRepository;
    private final ItineraryGetService itineraryGetService;

    private final SafariRepository safariRepository;

    private final TestimonyGetService testimonyGetService;
    private final HeroGetService heroGetService;

    private final IdObfuscator idObfuscator;

    // ========================
    // NAVIGATION
    // ========================

    public ResponseEntity<ApiResponse<?>> getNavigation() {
        try {
            Map<String, Object> nav = new HashMap<>();
            nav.put("parksCount", parkRepository.count(ParkSpecification.isActive(true)));
            nav.put("activitiesCount", activityRepository.count(
                Specification.where(ActivitySpecification.isActive(true))
                    .and(ActivitySpecification.isWebActive(true))
            ));
            nav.put("accommodationsCount", accommodationRepository.count(AccommodationSpecification.isActive(true)));
            nav.put("itinerariesCount", itineraryRepository.count(ItinerarySpecification.isActive(true)));
            nav.put("safarisCount", safariRepository.count(SafariSpecification.isActive(true)));
            return ResponseEntity.ok(ApiResponse.success(200, "Navigation data retrieved", nav));
        } catch (Exception e) {
            log.error("Error fetching navigation data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch navigation data", "NAVIGATION_FETCH_FAILED"));
        }
    }

    // ========================
    // PARKS
    // ========================

    public ResponseEntity<ApiResponse<?>> getParks(Integer page, Integer size, String sortBy, String sortDir,
                                                    String region, ParkType parkType, String keyword) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDir = sortDir != null ? sortDir : "asc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "name";

            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Park> spec = Specification.where(ParkSpecification.isActive(true));
            if (region != null && !region.isEmpty()) spec = spec.and(ParkSpecification.regionLike(region));
            if (parkType != null) spec = spec.and(ParkSpecification.hasParkType(parkType));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ParkSpecification.searchKeyword(keyword));

            Page<Park> parkPage = parkRepository.findAll(spec, pageable);
            List<ParkDTO> dtos = parkPage.getContent().stream()
                .map(parkGetService::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Parks retrieved", buildPageResponse("parks", dtos, parkPage)));
        } catch (Exception e) {
            log.error("Error fetching public parks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch parks", "PARKS_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getParkBySlug(String slug) {
        try {
            Park park = parkRepository.findBySlug(slug).orElse(null);
            if (park == null || !Boolean.TRUE.equals(park.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
            }
            return buildParkDetailResponse(park);
        } catch (Exception e) {
            log.error("Error fetching park by slug", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch park", "PARK_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getParkById(String id) {
        try {
            Long parkId = idObfuscator.decodeId(id);
            Park park = parkRepository.findById(parkId).orElse(null);
            if (park == null || !Boolean.TRUE.equals(park.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
            }
            return buildParkDetailResponse(park);
        } catch (Exception e) {
            log.error("Error fetching park by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch park", "PARK_FETCH_FAILED"));
        }
    }

    private ResponseEntity<ApiResponse<?>> buildParkDetailResponse(Park park) {
        ParkDTO dto = parkGetService.convertToDTO(park);
        List<ParkImageDTO> images = parkImageRepository.findActiveByParkId(park.getId()).stream()
            .map(parkImageGetService::toDTO)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("park", dto);
        response.put("images", images);
        return ResponseEntity.ok(ApiResponse.success(200, "Park retrieved", response));
    }

    // ========================
    // ACTIVITIES
    // ========================

    public ResponseEntity<ApiResponse<?>> getActivities(Integer page, Integer size, String sortBy, String sortDir,
                                                         String keyword) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDir = sortDir != null ? sortDir : "asc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "name";

            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Activity> spec = Specification.where(ActivitySpecification.isActive(true))
                .and(ActivitySpecification.isWebActive(true));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ActivitySpecification.searchKeyword(keyword));

            Page<Activity> activityPage = activityRepository.findAll(spec, pageable);
            List<ActivityDTO> dtos = activityPage.getContent().stream()
                .map(activityGetService::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Activities retrieved", buildPageResponse("activities", dtos, activityPage)));
        } catch (Exception e) {
            log.error("Error fetching public activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch activities", "ACTIVITIES_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getActivityBySlug(String slug) {
        try {
            Activity activity = activityRepository.findBySlug(slug).orElse(null);
            if (activity == null || !Boolean.TRUE.equals(activity.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
            }
            return buildActivityDetailResponse(activity);
        } catch (Exception e) {
            log.error("Error fetching activity by slug", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch activity", "ACTIVITY_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getActivityById(String id) {
        try {
            Long activityId = idObfuscator.decodeId(id);
            Activity activity = activityRepository.findById(activityId).orElse(null);
            if (activity == null || !Boolean.TRUE.equals(activity.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
            }
            return buildActivityDetailResponse(activity);
        } catch (Exception e) {
            log.error("Error fetching activity by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch activity", "ACTIVITY_FETCH_FAILED"));
        }
    }

    private ResponseEntity<ApiResponse<?>> buildActivityDetailResponse(Activity activity) {
        ActivityDTO dto = activityGetService.convertToDTO(activity);
        List<ActivityImageDTO> images = activityImageRepository
            .findByActivityIdAndIsActiveOrderByDisplayOrderAsc(activity.getId(), true).stream()
            .map(activityImageGetService::toDTO)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("activity", dto);
        response.put("images", images);
        return ResponseEntity.ok(ApiResponse.success(200, "Activity retrieved", response));
    }

    // ========================
    // ACCOMMODATIONS
    // ========================

    public ResponseEntity<ApiResponse<?>> getAccommodations(Integer page, Integer size, String sortBy, String sortDir,
                                                             String region, AccommodationType type,
                                                             AccommodationCategory category, String keyword) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDir = sortDir != null ? sortDir : "asc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "name";

            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Accommodation> spec = Specification.where(AccommodationSpecification.isActive(true));
            if (region != null && !region.isEmpty()) spec = spec.and(AccommodationSpecification.regionLike(region));
            if (type != null) spec = spec.and(AccommodationSpecification.hasAccommodationType(type));
            if (category != null) spec = spec.and(AccommodationSpecification.hasCategory(category));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(AccommodationSpecification.searchKeyword(keyword));

            Page<Accommodation> accommodationPage = accommodationRepository.findAll(spec, pageable);
            List<PublicAccommodationDTO> dtos = accommodationPage.getContent().stream()
                .map(this::convertToPublicAccommodationDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Accommodations retrieved", buildPageResponse("accommodations", dtos, accommodationPage)));
        } catch (Exception e) {
            log.error("Error fetching public accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch accommodations", "ACCOMMODATIONS_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAccommodationBySlug(String slug) {
        try {
            Accommodation accommodation = accommodationRepository.findBySlug(slug).orElse(null);
            if (accommodation == null || !Boolean.TRUE.equals(accommodation.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }
            return buildAccommodationDetailResponse(accommodation);
        } catch (Exception e) {
            log.error("Error fetching accommodation by slug", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch accommodation", "ACCOMMODATION_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getAccommodationById(String id) {
        try {
            Long accommodationId = idObfuscator.decodeId(id);
            Accommodation accommodation = accommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null || !Boolean.TRUE.equals(accommodation.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }
            return buildAccommodationDetailResponse(accommodation);
        } catch (Exception e) {
            log.error("Error fetching accommodation by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch accommodation", "ACCOMMODATION_FETCH_FAILED"));
        }
    }

    private ResponseEntity<ApiResponse<?>> buildAccommodationDetailResponse(Accommodation accommodation) {
        PublicAccommodationDTO dto = convertToPublicAccommodationDTO(accommodation);
        List<AccommodationImageDTO> images = accommodationImageRepository
            .findActiveByAccommodationId(accommodation.getId()).stream()
            .map(accommodationImageGetService::toDTO)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("accommodation", dto);
        response.put("images", images);
        return ResponseEntity.ok(ApiResponse.success(200, "Accommodation retrieved", response));
    }

    private PublicAccommodationDTO convertToPublicAccommodationDTO(Accommodation a) {
        return PublicAccommodationDTO.builder()
            .id(idObfuscator.encodeId(a.getId()))
            .name(a.getName())
            .slug(a.getSlug())
            .accommodationType(a.getAccommodationType())
            .accommodationTypeDisplayName(a.getAccommodationType() != null ? a.getAccommodationType().getDisplayName() : null)
            .accommodationTypeDescription(a.getAccommodationType() != null ? a.getAccommodationType().getDescription() : null)
            .category(a.getCategory())
            .categoryDisplayName(a.getCategory() != null ? a.getCategory().getDisplayName() : null)
            .categoryDescription(a.getCategory() != null ? a.getCategory().getDescription() : null)
            .categoryApproximateStars(a.getCategory() != null ? a.getCategory().getApproximateStars() : null)
            .logoUrl(a.getLogoUrl())
            .website(a.getWebsite())
            .hasBranch(a.getHasBranch())
            .isHeadquarters(a.getIsHeadquarters())
            .parentAccommodationId(a.getParentAccommodation() != null ? idObfuscator.encodeId(a.getParentAccommodation().getId()) : null)
            .parentAccommodationName(a.getParentAccommodation() != null ? a.getParentAccommodation().getName() : null)
            .region(a.getRegion())
            .district(a.getDistrict())
            .location(a.getLocation())
            .address(a.getAddress())
            .latitude(a.getLatitude())
            .longitude(a.getLongitude())
            .elevation(a.getElevation())
            .totalRooms(a.getTotalRooms())
            .totalBeds(a.getTotalBeds())
            .maxGuests(a.getMaxGuests())
            .starRating(a.getStarRating())
            .shortDescription(a.getShortDescription())
            .details(a.getDetails())
            .amenities(a.getAmenities())
            .services(a.getServices())
            .nearbyAttractions(a.getNearbyAttractions())
            .termsAndConditions(a.getTermsAndConditions())
            .cancellationPolicy(a.getCancellationPolicy())
            .checkInPolicy(a.getCheckInPolicy())
            .checkOutPolicy(a.getCheckOutPolicy())
            .childPolicy(a.getChildPolicy())
            .petPolicy(a.getPetPolicy())
            .priceRange(a.getPriceRange())
            .currency(a.getCurrency())
            .bestSeason(a.getBestSeason())
            .operatingSeason(a.getOperatingSeason())
            .tags(a.getTags())
            .isActive(a.getIsActive())
            .createdAt(a.getCreatedAt())
            .updatedAt(a.getUpdatedAt())
            .imageCount(a.getImages() != null ? a.getImages().size() : 0)
            .roomTypeCount(a.getRoomTypes() != null ? a.getRoomTypes().size() : 0)
            .roomStandardCount(a.getRoomStandards() != null ? a.getRoomStandards().size() : 0)
            .boardTypeCount(a.getBoardTypes() != null ? a.getBoardTypes().size() : 0)
            .build();
    }

    // ========================
    // ITINERARIES
    // ========================

    public ResponseEntity<ApiResponse<?>> getItineraries(Integer page, Integer size, String sortBy, String sortDir,
                                                          TripType tripType, BudgetCategory budgetCategory, String keyword) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDir = sortDir != null ? sortDir : "desc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt";

            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Itinerary> spec = Specification.where(ItinerarySpecification.isActive(true));
            if (tripType != null) spec = spec.and(ItinerarySpecification.hasTripType(tripType));
            if (budgetCategory != null) spec = spec.and(ItinerarySpecification.hasBudgetCategory(budgetCategory));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ItinerarySpecification.searchKeyword(keyword));

            Page<Itinerary> itineraryPage = itineraryRepository.findAll(spec, pageable);
            List<ItineraryDTO> dtos = itineraryPage.getContent().stream()
                .map(itineraryGetService::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Itineraries retrieved", buildPageResponse("itineraries", dtos, itineraryPage)));
        } catch (Exception e) {
            log.error("Error fetching public itineraries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch itineraries", "ITINERARIES_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getItineraryById(String id) {
        try {
            Long itineraryId = idObfuscator.decodeId(id);
            Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
            if (itinerary == null || !Boolean.TRUE.equals(itinerary.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND"));
            }

            ItineraryDTO dto = itineraryGetService.convertToDTO(itinerary);
            return ResponseEntity.ok(ApiResponse.success(200, "Itinerary retrieved", dto));
        } catch (Exception e) {
            log.error("Error fetching itinerary by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch itinerary", "ITINERARY_FETCH_FAILED"));
        }
    }

    // ========================
    // SAFARIS
    // ========================

    public ResponseEntity<ApiResponse<?>> getSafaris(Integer page, Integer size, String sortBy, String sortDir,
                                                      String keyword) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDir = sortDir != null ? sortDir : "desc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "startDate";

            Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Safari> spec = Specification.where(SafariSpecification.isActive(true));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(SafariSpecification.searchKeyword(keyword));

            Page<Safari> safariPage = safariRepository.findAll(spec, pageable);
            List<PublicSafariDTO> dtos = safariPage.getContent().stream()
                .map(this::convertToPublicSafariDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Safaris retrieved", buildPageResponse("safaris", dtos, safariPage)));
        } catch (Exception e) {
            log.error("Error fetching public safaris", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safaris", "SAFARIS_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getSafariBySlug(String slug) {
        try {
            Safari safari = safariRepository.findBySlug(slug).orElse(null);
            if (safari == null || !Boolean.TRUE.equals(safari.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Safari retrieved", convertToPublicSafariDTO(safari)));
        } catch (Exception e) {
            log.error("Error fetching safari by slug", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safari", "SAFARI_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getSafariById(String id) {
        try {
            Long safariId = idObfuscator.decodeId(id);
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null || !Boolean.TRUE.equals(safari.getIsActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Safari retrieved", convertToPublicSafariDTO(safari)));
        } catch (Exception e) {
            log.error("Error fetching safari by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch safari", "SAFARI_FETCH_FAILED"));
        }
    }

    private PublicSafariDTO convertToPublicSafariDTO(Safari safari) {
        PublicSafariDTO.PublicSafariDTOBuilder builder = PublicSafariDTO.builder()
            .id(idObfuscator.encodeId(safari.getId()))
            .name(safari.getName())
            .slug(safari.getSlug())
            .state(safari.getState())
            .stateDisplayName(safari.getState() != null ? safari.getState().getDisplayName() : null)
            .startDate(safari.getStartDate())
            .endDate(safari.getEndDate())
            .totalDays(safari.getTotalDays())
            .totalNights(safari.getTotalNights())
            .description(safari.getDescription())
            .highlights(safari.getHighlights())
            .startLocation(safari.getStartLocation())
            .endLocation(safari.getEndLocation())
            .isActive(safari.getIsActive())
            .totalPaxCount(safari.getTotalPaxCount())
            .totalDaysCount(safari.getDays() != null ? safari.getDays().size() : 0)
            .createdAt(safari.getCreatedAt())
            .updatedAt(safari.getUpdatedAt());

        var phase = safari.getCurrentPhase();
        builder.phase(phase)
            .phaseDisplayName(phase.getDisplayName());

        if (safari.getItinerary() != null) {
            builder.itineraryId(idObfuscator.encodeId(safari.getItinerary().getId()))
                .itineraryName(safari.getItinerary().getName());
        }

        return builder.build();
    }

    // ========================
    // TESTIMONIES (delegate)
    // ========================

    public ResponseEntity<ApiResponse<?>> getTestimonies() {
        return testimonyGetService.getPublicTestimonies();
    }

    public ResponseEntity<ApiResponse<?>> getFeaturedTestimonies() {
        return testimonyGetService.getFeaturedTestimonies();
    }

    // ========================
    // HEROES (delegate)
    // ========================

    public ResponseEntity<ApiResponse<?>> getHeroesByPage(HeroPage page) {
        return heroGetService.getHeroesByPage(page);
    }

    // ========================
    // UTILITY
    // ========================

    private <T> Map<String, Object> buildPageResponse(String key, List<?> items, Page<T> page) {
        Map<String, Object> response = new HashMap<>();
        response.put(key, items);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("pageSize", page.getSize());
        return response;
    }
}
