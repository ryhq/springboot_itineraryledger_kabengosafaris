package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.AccommodationDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.CreateAccommodationDTO;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * CreateAccommodationService - Service for creating and validating new accommodations
 *
 * This service handles:
 * - Request validation
 * - Duplicate name/slug checks
 * - Slug generation from name
 * - Parent accommodation validation for branches (using encoded IDs)
 * - Entity creation and persistence
 * - Response formatting with ApiResponse
 */
@Service
@Slf4j
public class CreateAccommodationService {

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository vendorRepository;

    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public CreateAccommodationService(
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new accommodation with validation
     *
     * @param createDTO The request DTO containing accommodation details
     * @return ResponseEntity with ApiResponse containing created accommodation or error
     */
    @Transactional
    @AuditLogAnnotation(
        action = "CREATE_ACCOMMODATION",
        description = "Creating a new accommodation",
        entityType = "Accommodation"
    )
    public ResponseEntity<ApiResponse<?>> createAccommodation(CreateAccommodationDTO createDTO) {
        log.info("Creating new accommodation: {}", createDTO.getName());

        try {
            // Generate slug from name if not provided
            String slug = createDTO.getSlug();
            if (slug == null || slug.isBlank()) {
                slug = generateSlug(createDTO.getName());
            } else {
                // Normalize provided slug
                slug = normalizeSlug(slug);
            }

            // Check for duplicate name
            if (accommodationRepository.existsByName(createDTO.getName())) {
                log.warn("Accommodation name already exists: {}", createDTO.getName());
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Accommodation name already exists",
                        "DUPLICATE_ACCOMMODATION_NAME"
                    )
                );
            }

            // Check for duplicate slug
            if (accommodationRepository.existsBySlug(slug)) {
                log.warn("Accommodation slug already exists: {}", slug);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Accommodation slug already exists. Please provide a unique slug or modify the name.",
                        "DUPLICATE_ACCOMMODATION_SLUG"
                    )
                );
            }

            // Validate parent accommodation if this is a branch
            Accommodation parentAccommodation = null;

            /* Who we pay for a stay here. Optional: often not known on day one. */
            com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor vendor = null;
            if (createDTO.getVendorId() != null
                    && !createDTO.getVendorId().isBlank()) {
                try {
                    vendor = vendorRepository
                        .findById(idObfuscator.decodeId(createDTO.getVendorId()))
                        .orElse(null);
                } catch (Exception e) {
                    vendor = null;
                }
                if (vendor == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Vendor not found", "VENDOR_NOT_FOUND")
                    );
                }
            }
            if (createDTO.getParentAccommodationId() != null && !createDTO.getParentAccommodationId().isBlank()) {
                try {
                    // Decode the parent accommodation ID
                    Long parentId = idObfuscator.decodeId(createDTO.getParentAccommodationId());
                    Optional<Accommodation> parentOpt = accommodationRepository.findById(parentId);

                    if (parentOpt.isEmpty()) {
                        log.warn("Parent accommodation not found for ID: {}", createDTO.getParentAccommodationId());
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(
                                400,
                                "Main location not found",
                                "PARENT_ACCOMMODATION_NOT_FOUND"
                            )
                        );
                    }

                    parentAccommodation = parentOpt.get();

                    // Validate: Check if parent has a valid parent chain (no existing circular issues)
                    if (hasCircularParentChain(parentAccommodation)) {
                        log.warn("Parent accommodation has circular reference in its parent chain: {}", parentId);
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(
                                400,
                                "Cannot link to this main location: it has a circular reference in its location chain",
                                "PARENT_HAS_CIRCULAR_REFERENCE"
                            )
                        );
                    }

                    // Update parent to indicate it has branches
                    if (!parentAccommodation.getHasBranch()) {
                        parentAccommodation.setHasBranch(true);
                        accommodationRepository.save(parentAccommodation);
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    log.warn("Invalid parent accommodation ID: {}", createDTO.getParentAccommodationId());
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid main location ID",
                            "INVALID_MAIN_LOCATION_ID"
                        )
                    );
                }
            }

            // Create accommodation entity
            Accommodation accommodation = Accommodation.builder()
                .name(createDTO.getName())
                .slug(slug)
                .accommodationType(createDTO.getAccommodationType())
                .category(createDTO.getCategory())
                .tin(createDTO.getTin())
                .vrn(createDTO.getVrn())
                .website(createDTO.getWebsite())
                .hasBranch(createDTO.getHasBranch() != null ? createDTO.getHasBranch() : false)
                .isHeadquarters(createDTO.getIsHeadquarters() != null ? createDTO.getIsHeadquarters() : true)
                .parentAccommodation(parentAccommodation)
                // who we pay for a stay here; optional, and set later if unknown
                .vendor(vendor)
                .region(createDTO.getRegion())
                .district(createDTO.getDistrict())
                .location(createDTO.getLocation())
                .address(createDTO.getAddress())
                .latitude(createDTO.getLatitude())
                .longitude(createDTO.getLongitude())
                .elevation(createDTO.getElevation())
                .totalRooms(createDTO.getTotalRooms())
                .totalBeds(createDTO.getTotalBeds())
                .maxGuests(createDTO.getMaxGuests())
                .starRating(createDTO.getStarRating())
                .shortDescription(createDTO.getShortDescription())
                .details(createDTO.getDetails())
                .amenities(createDTO.getAmenities())
                .services(createDTO.getServices())
                .nearbyAttractions(createDTO.getNearbyAttractions())
                .termsAndConditions(createDTO.getTermsAndConditions())
                .cancellationPolicy(createDTO.getCancellationPolicy())
                .checkInPolicy(createDTO.getCheckInPolicy())
                .checkOutPolicy(createDTO.getCheckOutPolicy())
                .childPolicy(createDTO.getChildPolicy())
                .petPolicy(createDTO.getPetPolicy())
                .priceRange(createDTO.getPriceRange())
                .currency(createDTO.getCurrency() != null ? createDTO.getCurrency() : "USD")
                .bestSeason(createDTO.getBestSeason())
                .operatingSeason(createDTO.getOperatingSeason())
                .tags(createDTO.getTags())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .isWebActive(createDTO.getIsWebActive() != null ? createDTO.getIsWebActive() : true)
                .internalNotes(createDTO.getInternalNotes())
                .build();

            // Save to database
            Accommodation savedAccommodation = accommodationRepository.save(accommodation);

            log.info("Accommodation created successfully with ID: {}", savedAccommodation.getId());

            // Create response DTO
            AccommodationDTO accommodationDTO = convertToDTO(savedAccommodation);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Accommodation created successfully",
                    accommodationDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create accommodation: " + e.getMessage(),
                    "ACCOMMODATION_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Generate URL-friendly slug from accommodation name
     *
     * @param name The accommodation name
     * @return URL-friendly slug
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
            .trim()
            .replaceAll("\\s+", "-")           // Replace spaces with hyphens
            .replaceAll("[^a-z0-9-]", "")      // Remove non-alphanumeric except hyphens
            .replaceAll("-+", "-")             // Replace multiple hyphens with single
            .replaceAll("^-|-$", "");          // Remove leading/trailing hyphens
    }

    /**
     * Normalize a provided slug
     *
     * @param slug The slug to normalize
     * @return Normalized slug
     */
    private String normalizeSlug(String slug) {
        return slug.toLowerCase()
            .trim()
            .replaceAll("\\s+", "-")
            .replaceAll("[^a-z0-9-]", "")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }

    /**
     * Check if an accommodation has a circular reference in its parent chain
     *
     * This validates that the parent accommodation's existing parent chain doesn't
     * have any circular references. This is used during creation to ensure we're
     * not adding a branch to an already corrupted hierarchy.
     *
     * @param accommodation The accommodation to check
     * @return true if circular reference exists in parent chain, false otherwise
     */
    private boolean hasCircularParentChain(Accommodation accommodation) {
        Accommodation current = accommodation.getParentAccommodation();
        int maxDepth = 100; // Safety limit to prevent infinite loops
        int depth = 0;

        // Track visited accommodations to detect circular references
        java.util.Set<Long> visited = new java.util.HashSet<>();
        visited.add(accommodation.getId());

        while (current != null && depth < maxDepth) {
            // If we've seen this accommodation before in the chain, it's circular
            if (visited.contains(current.getId())) {
                log.warn("Circular reference detected in parent chain of accommodation: {}",
                    accommodation.getId());
                return true;
            }

            visited.add(current.getId());
            current = current.getParentAccommodation();
            depth++;
        }

        if (depth >= maxDepth) {
            log.error("Maximum parent chain depth exceeded for accommodation: {}. Possible circular reference.",
                accommodation.getId());
            return true; // Treat as circular to be safe
        }

        return false;
    }

    /**
     * Convert Accommodation entity to DTO
     *
     * @param accommodation The accommodation entity
     * @return AccommodationDTO
     */
    private AccommodationDTO convertToDTO(Accommodation accommodation) {
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
            .vendorId(accommodation.getVendor() != null
                ? idObfuscator.encodeId(accommodation.getVendor().getId()) : null)
            .vendorName(accommodation.getVendor() != null
                ? accommodation.getVendor().getName() : null)
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
            .isWebActive(accommodation.getIsWebActive())
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
}
