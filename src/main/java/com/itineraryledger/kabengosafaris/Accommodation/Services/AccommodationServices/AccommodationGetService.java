package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.AccommodationDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.AccommodationListItemDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationCategory;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AccommodationGetService - Service for retrieving accommodations with filtering, pagination, and sorting
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccommodationGetService {

    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "slug", "accommodationType", "category", "region", "district",
        "starRating", "totalRooms", "maxGuests", "isActive", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public AccommodationGetService(
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get a single accommodation by obfuscated ID
     *
     * @param idObfuscated The obfuscated accommodation ID
     * @return ResponseEntity with ApiResponse containing the accommodation
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationById(String idObfuscated) {
        log.info("Fetching accommodation with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode accommodation ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid accommodation ID",
                        "INVALID_ACCOMMODATION_ID"
                    )
                );
            }

            // Find accommodation
            Accommodation accommodation = accommodationRepository.findById(id).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Accommodation not found",
                        "ACCOMMODATION_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            AccommodationDTO accommodationDTO = convertToDTO(accommodation);

            // Circular navigation
            Long nextId = accommodationRepository.findNextId(id).orElse(null);
            Long previousId = accommodationRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = accommodationRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = accommodationRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("accommodation", accommodationDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation",
                    "ACCOMMODATION_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get a single accommodation by slug
     *
     * @param slug The accommodation slug
     * @return ResponseEntity with ApiResponse containing the accommodation
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationBySlug(String slug) {
        log.info("Fetching accommodation with slug: {}", slug);

        try {
            // Find accommodation
            Accommodation accommodation = accommodationRepository.findBySlug(slug).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(
                        404,
                        "Accommodation not found",
                        "ACCOMMODATION_NOT_FOUND"
                    )
                );
            }

            // Convert to DTO
            AccommodationDTO accommodationDTO = convertToDTO(accommodation);

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodation retrieved successfully",
                    accommodationDTO
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodation",
                    "ACCOMMODATION_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Get all accommodations with pagination, sorting, and filtering
     *
     * @param name Filter by name (partial match)
     * @param slug Filter by slug (partial match)
     * @param accommodationType Filter by accommodation type
     * @param category Filter by category
     * @param region Filter by region (partial match)
     * @param exactRegion Filter by exact region
     * @param district Filter by district (partial match)
     * @param exactDistrict Filter by exact district
     * @param starRating Filter by exact star rating
     * @param minStarRating Filter by minimum star rating
     * @param maxStarRating Filter by maximum star rating
     * @param isActive Filter by active status
     * @param isHeadquarters Filter by headquarters status
     * @param hasBranch Filter by has branch status
     * @param parentId Filter by parent accommodation ID
     * @param hasNoParent Filter accommodations with no parent
     * @param minRooms Filter by minimum rooms
     * @param maxRooms Filter by maximum rooms
     * @param minGuests Filter by minimum guest capacity
     * @param maxGuests Filter by maximum guest capacity
     * @param tags Filter by tags (partial match)
     * @param amenities Filter by amenities (partial match)
     * @param hasImages Filter accommodations with images
     * @param hasRates Filter accommodations with rates
     * @param hasCoordinates Filter accommodations with GPS coordinates
     * @param keyword Search keyword across multiple fields
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortDirection Sort direction (asc/desc)
     * @return ResponseEntity with ApiResponse containing paginated accommodations
     */
    public ResponseEntity<ApiResponse<?>> getAllAccommodations(
        String name,
        String slug,
        AccommodationType accommodationType,
        AccommodationCategory category,
        String region,
        String exactRegion,
        String district,
        String exactDistrict,
        Integer starRating,
        Integer minStarRating,
        Integer maxStarRating,
        Boolean isActive,
        Boolean isHeadquarters,
        Boolean hasBranch,
        String parentId,
        Boolean hasNoParent,
        Integer minRooms,
        Integer maxRooms,
        Integer minGuests,
        Integer maxGuests,
        String tags,
        String amenities,
        Boolean hasImages,
        Boolean hasRates,
        Boolean hasCoordinates,
        String keyword,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        log.info("Fetching all accommodations with filters");

        try {
            // Build specification for filtering
            Specification<Accommodation> spec = Specification.unrestricted();

            if (name != null && !name.isEmpty()) {
                spec = spec.and(AccommodationSpecification.nameLike(name));
            }
            if (slug != null && !slug.isEmpty()) {
                spec = spec.and(AccommodationSpecification.slugLike(slug));
            }
            if (accommodationType != null) {
                spec = spec.and(AccommodationSpecification.hasAccommodationType(accommodationType));
            }
            if (category != null) {
                spec = spec.and(AccommodationSpecification.hasCategory(category));
            }
            if (region != null && !region.isEmpty()) {
                spec = spec.and(AccommodationSpecification.regionLike(region));
            }
            if (exactRegion != null && !exactRegion.isEmpty()) {
                spec = spec.and(AccommodationSpecification.hasRegion(exactRegion));
            }
            if (district != null && !district.isEmpty()) {
                spec = spec.and(AccommodationSpecification.districtLike(district));
            }
            if (exactDistrict != null && !exactDistrict.isEmpty()) {
                spec = spec.and(AccommodationSpecification.hasDistrict(exactDistrict));
            }
            if (starRating != null) {
                spec = spec.and(AccommodationSpecification.hasStarRating(starRating));
            }
            if (minStarRating != null) {
                spec = spec.and(AccommodationSpecification.minStarRating(minStarRating));
            }
            if (maxStarRating != null) {
                spec = spec.and(AccommodationSpecification.maxStarRating(maxStarRating));
            }
            if (isActive != null) {
                spec = spec.and(AccommodationSpecification.isActive(isActive));
            }
            if (isHeadquarters != null) {
                spec = spec.and(AccommodationSpecification.isHeadquarters(isHeadquarters));
            }
            if (hasBranch != null) {
                spec = spec.and(AccommodationSpecification.hasBranch(hasBranch));
            }
            if (parentId != null && !parentId.isEmpty()) {
                try {
                    Long decodedParentId = idObfuscator.decodeId(parentId);
                    spec = spec.and(AccommodationSpecification.hasParentId(decodedParentId));
                } catch (Exception e) {
                    log.warn("Failed to decode parent ID: {}", parentId, e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid parent accommodation ID",
                            "INVALID_PARENT_ID"
                        )
                    );
                }
            }
            if (hasNoParent != null && hasNoParent) {
                spec = spec.and(AccommodationSpecification.hasNoParent());
            }
            if (minRooms != null) {
                spec = spec.and(AccommodationSpecification.minTotalRooms(minRooms));
            }
            if (maxRooms != null) {
                spec = spec.and(AccommodationSpecification.maxTotalRooms(maxRooms));
            }
            if (minGuests != null) {
                spec = spec.and(AccommodationSpecification.minMaxGuests(minGuests));
            }
            if (maxGuests != null) {
                spec = spec.and(AccommodationSpecification.maxMaxGuests(maxGuests));
            }
            if (tags != null && !tags.isEmpty()) {
                spec = spec.and(AccommodationSpecification.tagsLike(tags));
            }
            if (amenities != null && !amenities.isEmpty()) {
                spec = spec.and(AccommodationSpecification.amenitiesLike(amenities));
            }
            if (hasImages != null && hasImages) {
                spec = spec.and(AccommodationSpecification.hasImages());
            }
            if (hasRates != null && hasRates) {
                spec = spec.and(AccommodationSpecification.hasRates());
            }
            if (hasCoordinates != null && hasCoordinates) {
                spec = spec.and(AccommodationSpecification.hasCoordinates());
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(AccommodationSpecification.searchKeyword(keyword));
            }

            // Set default pagination values
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 10;

            // Sorting with validation
            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                log.warn("Invalid sort field: {}", sortBy);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort.Direction direction = Sort.Direction.DESC;
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }

            // Create pageable
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, validatedSortBy));

            // Fetch accommodations
            Page<Accommodation> accommodationPage = accommodationRepository.findAll(spec, pageable);

            // Convert to DTOs
            List<AccommodationDTO> accommodationDTOs = accommodationPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

            // Create response with pagination metadata
            Map<String, Object> response = new HashMap<>();
            response.put("accommodations", accommodationDTOs);
            response.put("currentPage", accommodationPage.getNumber());
            response.put("totalItems", accommodationPage.getTotalElements());
            response.put("totalPages", accommodationPage.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDir", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodations retrieved successfully",
                    response
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodations",
                    "ACCOMMODATIONS_FETCH_FAILED"
                )
            );
        }
    }

    /**
     * Convert Accommodation entity to AccommodationDTO
     */
    public AccommodationDTO convertToDTO(Accommodation accommodation) {
        return AccommodationDTO.builder()
            .id(idObfuscator.encodeId(accommodation.getId()))
            .name(accommodation.getName())
            .slug(accommodation.getSlug())
            .accommodationType(accommodation.getAccommodationType())
            .accommodationTypeDisplayName(accommodation.getAccommodationType() != null ?
                accommodation.getAccommodationType().getDisplayName() : null)
            .accommodationTypeDescription(accommodation.getAccommodationType() != null ?
                accommodation.getAccommodationType().getDescription() : null)
            .category(accommodation.getCategory())
            .categoryDisplayName(accommodation.getCategory() != null ?
                accommodation.getCategory().getDisplayName() : null)
            .categoryDescription(accommodation.getCategory() != null ?
                accommodation.getCategory().getDescription() : null)
            .categoryApproximateStars(accommodation.getCategory() != null ?
                accommodation.getCategory().getApproximateStars() : null)
            .tin(accommodation.getTin())
            .vrn(accommodation.getVrn())
            .logoUrl(accommodation.getLogoUrl())
            .website(accommodation.getWebsite())
            .hasBranch(accommodation.getHasBranch())
            .isHeadquarters(accommodation.getIsHeadquarters())
            .parentAccommodationId(accommodation.getParentAccommodation() != null ?
                idObfuscator.encodeId(accommodation.getParentAccommodation().getId()) : null)
            .parentAccommodationName(accommodation.getParentAccommodation() != null ?
                accommodation.getParentAccommodation().getName() : null)
            .region(accommodation.getRegion())
            .district(accommodation.getDistrict())
            .location(accommodation.getLocation())
            .address(accommodation.getAddress())
            .latitude(accommodation.getLatitude())
            .longitude(accommodation.getLongitude())
            .elevation(accommodation.getElevation())
            .totalRooms(accommodation.getTotalRooms())
            .totalBeds(accommodation.getTotalBeds())
            .maxGuests(accommodation.getMaxGuests())
            .starRating(accommodation.getStarRating())
            .shortDescription(accommodation.getShortDescription())
            .details(accommodation.getDetails())
            .amenities(accommodation.getAmenities())
            .services(accommodation.getServices())
            .nearbyAttractions(accommodation.getNearbyAttractions())
            .termsAndConditions(accommodation.getTermsAndConditions())
            .cancellationPolicy(accommodation.getCancellationPolicy())
            .checkInPolicy(accommodation.getCheckInPolicy())
            .checkOutPolicy(accommodation.getCheckOutPolicy())
            .childPolicy(accommodation.getChildPolicy())
            .petPolicy(accommodation.getPetPolicy())
            .priceRange(accommodation.getPriceRange())
            .currency(accommodation.getCurrency())
            .bestSeason(accommodation.getBestSeason())
            .operatingSeason(accommodation.getOperatingSeason())
            .tags(accommodation.getTags())
            .isActive(accommodation.getIsActive())
            .createdAt(accommodation.getCreatedAt())
            .updatedAt(accommodation.getUpdatedAt())
            .emailCount(accommodation.getEmails() != null ? accommodation.getEmails().size() : 0)
            .phoneCount(accommodation.getPhones() != null ? accommodation.getPhones().size() : 0)
            .imageCount(accommodation.getImages() != null ? accommodation.getImages().size() : 0)
            .branchCount(accommodation.getBranches() != null ? accommodation.getBranches().size() : 0)
            .roomTypeCount(accommodation.getRoomTypes() != null ? accommodation.getRoomTypes().size() : 0)
            .roomStandardCount(accommodation.getRoomStandards() != null ? accommodation.getRoomStandards().size() : 0)
            .boardTypeCount(accommodation.getBoardTypes() != null ? accommodation.getBoardTypes().size() : 0)
            .rateCount(accommodation.getRates() != null ? accommodation.getRates().size() : 0)
            .documentCount(accommodation.getDocuments() != null ? accommodation.getDocuments().size() : 0)
            .build();
    }
    
    /**
     * Get lightweight list of accommodations for dropdown/select purposes
     * Returns only essential fields (id, name, location, region, isActive)
     *
     * @param hasBranch Optional filter for accommodations with branches
     * @return ResponseEntity with ApiResponse containing list of accommodations
     */
    public ResponseEntity<ApiResponse<?>> getAccommodationsList(Boolean hasBranch) {
        log.info("Fetching accommodations list for dropdown with hasBranch filter: {}", hasBranch);

        try {
            // Build specification
            Specification<Accommodation> spec = Specification.unrestricted();

            // Filter by hasBranch if provided
            if (hasBranch != null) {
                spec = spec.and(AccommodationSpecification.hasBranch(hasBranch));
            }

            // Fetch all matching accommodations sorted by name
            Sort sort = Sort.by("name").ascending();
            List<Accommodation> accommodations = accommodationRepository.findAll(spec, sort);

            // Convert to lightweight DTOs
            List<AccommodationListItemDTO> accommodationList = accommodations.stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(
                    200,
                    "Accommodations list retrieved successfully",
                    accommodationList
                )
            );

        } catch (Exception e) {
            log.error("Error fetching accommodations list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to fetch accommodations list",
                    "ACCOMMODATIONS_LIST_FETCH_FAILED"
                )
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    /**
     * Convert Accommodation entity to lightweight list item DTO
     */
    private AccommodationListItemDTO convertToListItemDTO(Accommodation accommodation) {
        return AccommodationListItemDTO.builder()
            .id(idObfuscator.encodeId(accommodation.getId()))
            .name(accommodation.getName())
            .location(accommodation.getLocation())
            .region(accommodation.getRegion())
            .isActive(accommodation.getIsActive())
            .build();
    }
}
