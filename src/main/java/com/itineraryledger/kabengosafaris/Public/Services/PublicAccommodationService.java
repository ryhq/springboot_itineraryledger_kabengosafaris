package com.itineraryledger.kabengosafaris.Public.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices.AccommodationSpecification;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicAccommodationDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicAccommodationListDTO;
import com.itineraryledger.kabengosafaris.Public.DTOs.PublicImageDTO;
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
public class PublicAccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageRepository accommodationImageRepository;
    private final PublicEntityResolver entityResolver;
    private final PublicImageResolver imageResolver;
    private final IdObfuscator idObfuscator;
    private final PublicTranslationService publicTranslationService;

    public ResponseEntity<ApiResponse<?>> getAccommodations(Integer page, Integer size, String sortBy, String sortDirection,
                                                             String region, AccommodationType type,
                                                             AccommodationCategory category, String keyword, String lang) {
        try {
            page = page != null ? page : 0;
            size = size != null ? size : 20;
            sortDirection = sortDirection != null ? sortDirection : "asc";
            sortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "name";

            Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Specification<Accommodation> spec = AccommodationSpecification.isActive(true).and(AccommodationSpecification.isWebActive(true));
            if (region != null && !region.isEmpty()) spec = spec.and(AccommodationSpecification.regionLike(region));
            if (type != null) spec = spec.and(AccommodationSpecification.hasAccommodationType(type));
            if (category != null) spec = spec.and(AccommodationSpecification.hasCategory(category));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(AccommodationSpecification.searchKeyword(keyword));

            Page<Accommodation> accommodationPage = accommodationRepository.findAll(spec, pageable);
            List<PublicAccommodationListDTO> dtos = accommodationPage.getContent().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());

            publicTranslationService.translateDtoList(dtos, lang);

            return ResponseEntity.ok(ApiResponse.success(200, "Accommodations retrieved", PublicServiceUtils.buildPageResponse("accommodations", dtos, accommodationPage)));
        } catch (Exception e) {
            log.error("Error fetching public accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch accommodations", "ACCOMMODATIONS_FETCH_FAILED"));
        }
    }

    /**
     * Get accommodation by identifier (obfuscated id or slug)
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationByIdentifier(String identifier, String lang) {
        try {
            Accommodation accommodation = entityResolver.resolveAccommodation(identifier).orElse(null);
            if (accommodation == null || !Boolean.TRUE.equals(accommodation.getIsActive()) || !Boolean.TRUE.equals(accommodation.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }
            return buildAccommodationDetailResponse(accommodation, lang);
        } catch (Exception e) {
            log.error("Error fetching accommodation by identifier: {}", identifier, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch accommodation", "ACCOMMODATION_FETCH_FAILED"));
        }
    }

    /**
     * Get paginated images for an accommodation (by identifier)
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationImages(String identifier, Integer page, Integer size) {
        try {
            Accommodation accommodation = entityResolver.resolveAccommodation(identifier).orElse(null);
            if (accommodation == null || !Boolean.TRUE.equals(accommodation.getIsActive()) || !Boolean.TRUE.equals(accommodation.getIsWebActive())) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }

            page = page != null ? page : 0;
            size = size != null ? size : 6;
            Pageable pageable = PageRequest.of(page, size);

            Page<AccommodationImage> imagePage = accommodationImageRepository.findByAccommodationIdPaginated(accommodation.getId(), pageable);
            List<PublicImageDTO> dtos = imagePage.getContent().stream()
                .map(imageResolver::toPublicDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(200, "Accommodation images retrieved", PublicServiceUtils.buildPageResponse("images", dtos, imagePage)));
        } catch (Exception e) {
            log.error("Error fetching accommodation images", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Failed to fetch accommodation images", "ACCOMMODATION_IMAGES_FETCH_FAILED"));
        }
    }

    private ResponseEntity<ApiResponse<?>> buildAccommodationDetailResponse(Accommodation accommodation, String lang) {
        PublicAccommodationDTO dto = convertToDetailDTO(accommodation);

        Pageable firstPage = PageRequest.of(0, 6);
        Page<AccommodationImage> imagePage = accommodationImageRepository.findByAccommodationIdPaginated(accommodation.getId(), firstPage);
        List<PublicImageDTO> images = imagePage.getContent().stream()
            .map(imageResolver::toPublicDTO)
            .collect(Collectors.toList());

        long totalImages = imagePage.getTotalElements();

        publicTranslationService.translateDto(dto, lang);

        Map<String, Object> response = new HashMap<>();
        response.put("accommodation", dto);
        response.put("images", images);
        response.put("totalImages", totalImages);
        return ResponseEntity.ok(ApiResponse.success(200, "Accommodation retrieved", response));
    }

    private PublicAccommodationListDTO convertToListDTO(Accommodation a) {
        return PublicAccommodationListDTO.builder()
            .id(idObfuscator.encodeId(a.getId()))
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
            .build();
    }

    private PublicAccommodationDTO convertToDetailDTO(Accommodation a) {
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
            .primaryImageUrl(imageResolver.resolveAccommodationImage(a.getId()))
            .imageCount(a.getImages() != null ? a.getImages().size() : 0)
            .roomTypeCount(a.getRoomTypes() != null ? a.getRoomTypes().size() : 0)
            .roomStandardCount(a.getRoomStandards() != null ? a.getRoomStandards().size() : 0)
            .boardTypeCount(a.getBoardTypes() != null ? a.getBoardTypes().size() : 0)
            .build();
    }
}
