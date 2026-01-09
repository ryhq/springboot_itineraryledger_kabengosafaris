package com.itineraryledger.kabengosafaris.Accommodation.Services.AccommodationServices;

import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.AccommodationDTO;
import com.itineraryledger.kabengosafaris.Accommodation.DTOs.AccommodationDTOs.UpdateAccommodationDTO;
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
 * UpdateAccommodationService - Service for updating accommodations
 */
@Service
@Slf4j
@Transactional
public class UpdateAccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public UpdateAccommodationService(
        AccommodationRepository accommodationRepository,
        IdObfuscator idObfuscator
    ) {
        this.accommodationRepository = accommodationRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an accommodation by obfuscated ID
     *
     * @param idObfuscated The obfuscated accommodation ID
     * @param updateAccommodationDTO The updated accommodation data
     * @return ResponseEntity with ApiResponse containing the updated accommodation
     */
    @AuditLogAnnotation(
        action = "UPDATE_ACCOMMODATION",
        description = "Updating accommodation",
        entityType = "Accommodation",
        entityIdParamName = "idObfuscated"
    )
    public ResponseEntity<ApiResponse<?>> updateAccommodation(String idObfuscated, UpdateAccommodationDTO updateAccommodationDTO) {
        log.info("Updating accommodation with ID: {}", idObfuscated);

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

            return updateAccommodationById(id, updateAccommodationDTO);

        } catch (Exception e) {
            log.error("Error updating accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update accommodation",
                    "ACCOMMODATION_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update an accommodation by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateAccommodationById(Long id, UpdateAccommodationDTO updateAccommodationDTO) {
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

        // Check if name is being changed and if it's unique
        if (updateAccommodationDTO.getName() != null && !updateAccommodationDTO.getName().equals(accommodation.getName())) {
            if (accommodationRepository.existsByName(updateAccommodationDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Accommodation with name '" + updateAccommodationDTO.getName() + "' already exists",
                        "ACCOMMODATION_NAME_EXISTS"
                    )
                );
            }
            accommodation.setName(updateAccommodationDTO.getName());
        }

        // Check if slug is being changed and if it's unique
        if (updateAccommodationDTO.getSlug() != null && !updateAccommodationDTO.getSlug().equals(accommodation.getSlug())) {
            if (accommodationRepository.existsBySlug(updateAccommodationDTO.getSlug())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Accommodation with slug '" + updateAccommodationDTO.getSlug() + "' already exists",
                        "ACCOMMODATION_SLUG_EXISTS"
                    )
                );
            }
            accommodation.setSlug(updateAccommodationDTO.getSlug());
        }

        // Update basic information
        if (updateAccommodationDTO.getAccommodationType() != null) {
            accommodation.setAccommodationType(updateAccommodationDTO.getAccommodationType());
        }
        if (updateAccommodationDTO.getCategory() != null) {
            accommodation.setCategory(updateAccommodationDTO.getCategory());
        }

        // Update business information
        if (updateAccommodationDTO.getTin() != null) {
            accommodation.setTin(updateAccommodationDTO.getTin());
        }
        if (updateAccommodationDTO.getVrn() != null) {
            accommodation.setVrn(updateAccommodationDTO.getVrn());
        }
        if (updateAccommodationDTO.getLogoUrl() != null) {
            accommodation.setLogoUrl(updateAccommodationDTO.getLogoUrl());
        }
        if (updateAccommodationDTO.getWebsite() != null) {
            accommodation.setWebsite(updateAccommodationDTO.getWebsite());
        }

        // Update multi-branch support
        if (updateAccommodationDTO.getHasBranch() != null) {
            accommodation.setHasBranch(updateAccommodationDTO.getHasBranch());
        }
        if (updateAccommodationDTO.getIsHeadquarters() != null) {
            accommodation.setIsHeadquarters(updateAccommodationDTO.getIsHeadquarters());
        }

        // Update parent accommodation if provided
        if (updateAccommodationDTO.getParentAccommodationId() != null) {
            if (updateAccommodationDTO.getParentAccommodationId().isBlank()) {
                // Clear parent accommodation
                accommodation.setParentAccommodation(null);
            } else {
                try {
                    Long parentId = idObfuscator.decodeId(updateAccommodationDTO.getParentAccommodationId());

                    // Validate: Accommodation cannot be its own parent
                    if (parentId.equals(id)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(
                                400,
                                "An accommodation cannot link to itself as main location",
                                "SELF_PARENT_NOT_ALLOWED"
                            )
                        );
                    }

                    Optional<Accommodation> parentOpt = accommodationRepository.findById(parentId);

                    if (parentOpt.isEmpty()) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(
                                400,
                                "Main location not found",
                                "PARENT_ACCOMMODATION_NOT_FOUND"
                            )
                        );
                    }

                    Accommodation parentAccommodation = parentOpt.get();

                    // Validate: Check for circular parent-child relationship
                    // This prevents scenarios like: A->B, B->C, C->A
                    if (wouldCreateCircularReference(parentAccommodation, id)) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(
                                400,
                                "Cannot link to this main location: it would create a circular relationship",
                                "CIRCULAR_PARENT_CHILD_REFERENCE"
                            )
                        );
                    }

                    // Update parent to indicate it has branches
                    if (!parentAccommodation.getHasBranch()) {
                        parentAccommodation.setHasBranch(true);
                        accommodationRepository.save(parentAccommodation);
                    }

                    accommodation.setParentAccommodation(parentAccommodation);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            400,
                            "Invalid main location ID",
                            "INVALID_MAIN_LOCATION_ID"
                        )
                    );
                }
            }
        }

        // Update location information
        if (updateAccommodationDTO.getRegion() != null) {
            accommodation.setRegion(updateAccommodationDTO.getRegion());
        }
        if (updateAccommodationDTO.getDistrict() != null) {
            accommodation.setDistrict(updateAccommodationDTO.getDistrict());
        }
        if (updateAccommodationDTO.getLocation() != null) {
            accommodation.setLocation(updateAccommodationDTO.getLocation());
        }
        if (updateAccommodationDTO.getAddress() != null) {
            accommodation.setAddress(updateAccommodationDTO.getAddress());
        }

        // Update GPS coordinates
        if (updateAccommodationDTO.getLatitude() != null) {
            accommodation.setLatitude(updateAccommodationDTO.getLatitude());
        }
        if (updateAccommodationDTO.getLongitude() != null) {
            accommodation.setLongitude(updateAccommodationDTO.getLongitude());
        }
        if (updateAccommodationDTO.getElevation() != null) {
            accommodation.setElevation(updateAccommodationDTO.getElevation());
        }

        // Update capacity and facilities
        if (updateAccommodationDTO.getTotalRooms() != null) {
            accommodation.setTotalRooms(updateAccommodationDTO.getTotalRooms());
        }
        if (updateAccommodationDTO.getTotalBeds() != null) {
            accommodation.setTotalBeds(updateAccommodationDTO.getTotalBeds());
        }
        if (updateAccommodationDTO.getMaxGuests() != null) {
            accommodation.setMaxGuests(updateAccommodationDTO.getMaxGuests());
        }
        if (updateAccommodationDTO.getStarRating() != null) {
            accommodation.setStarRating(updateAccommodationDTO.getStarRating());
        }

        // Update description and content
        if (updateAccommodationDTO.getShortDescription() != null) {
            accommodation.setShortDescription(updateAccommodationDTO.getShortDescription());
        }
        if (updateAccommodationDTO.getDetails() != null) {
            accommodation.setDetails(updateAccommodationDTO.getDetails());
        }
        if (updateAccommodationDTO.getAmenities() != null) {
            accommodation.setAmenities(updateAccommodationDTO.getAmenities());
        }
        if (updateAccommodationDTO.getServices() != null) {
            accommodation.setServices(updateAccommodationDTO.getServices());
        }
        if (updateAccommodationDTO.getNearbyAttractions() != null) {
            accommodation.setNearbyAttractions(updateAccommodationDTO.getNearbyAttractions());
        }

        // Update policies and terms
        if (updateAccommodationDTO.getTermsAndConditions() != null) {
            accommodation.setTermsAndConditions(updateAccommodationDTO.getTermsAndConditions());
        }
        if (updateAccommodationDTO.getCancellationPolicy() != null) {
            accommodation.setCancellationPolicy(updateAccommodationDTO.getCancellationPolicy());
        }
        if (updateAccommodationDTO.getCheckInPolicy() != null) {
            accommodation.setCheckInPolicy(updateAccommodationDTO.getCheckInPolicy());
        }
        if (updateAccommodationDTO.getCheckOutPolicy() != null) {
            accommodation.setCheckOutPolicy(updateAccommodationDTO.getCheckOutPolicy());
        }
        if (updateAccommodationDTO.getChildPolicy() != null) {
            accommodation.setChildPolicy(updateAccommodationDTO.getChildPolicy());
        }
        if (updateAccommodationDTO.getPetPolicy() != null) {
            accommodation.setPetPolicy(updateAccommodationDTO.getPetPolicy());
        }

        // Update pricing information
        if (updateAccommodationDTO.getPriceRange() != null) {
            accommodation.setPriceRange(updateAccommodationDTO.getPriceRange());
        }
        if (updateAccommodationDTO.getCurrency() != null) {
            accommodation.setCurrency(updateAccommodationDTO.getCurrency());
        }

        // Update seasonal information
        if (updateAccommodationDTO.getBestSeason() != null) {
            accommodation.setBestSeason(updateAccommodationDTO.getBestSeason());
        }
        if (updateAccommodationDTO.getOperatingSeason() != null) {
            accommodation.setOperatingSeason(updateAccommodationDTO.getOperatingSeason());
        }

        // Update tags
        if (updateAccommodationDTO.getTags() != null) {
            accommodation.setTags(updateAccommodationDTO.getTags());
        }

        // Update status
        if (updateAccommodationDTO.getIsActive() != null) {
            accommodation.setIsActive(updateAccommodationDTO.getIsActive());
        }

        // Update internal notes
        if (updateAccommodationDTO.getInternalNotes() != null) {
            accommodation.setInternalNotes(updateAccommodationDTO.getInternalNotes());
        }

        // Save updated accommodation
        accommodation = accommodationRepository.save(accommodation);

        // Convert to DTO
        AccommodationDTO accommodationDTO = convertToDTO(accommodation);

        log.info("Accommodation updated successfully: {}", accommodation.getName());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Accommodation updated successfully",
                accommodationDTO
            )
        );
    }

    /**
     * Check if setting a parent would create a circular reference
     *
     * This recursively checks if the proposed parent has the current accommodation
     * anywhere in its parent chain.
     *
     * Example scenarios prevented:
     * - A tries to set parent as B, but B's parent is A (direct circular)
     * - A tries to set parent as B, but B->C->A (indirect circular chain)
     *
     * @param proposedParent The accommodation being set as parent
     * @param currentAccommodationId The ID of the accommodation being updated
     * @return true if circular reference would be created, false otherwise
     */
    private boolean wouldCreateCircularReference(Accommodation proposedParent, Long currentAccommodationId) {
        // Traverse up the parent chain of the proposed parent
        Accommodation current = proposedParent;
        int maxDepth = 100; // Safety limit to prevent infinite loops in case of data corruption
        int depth = 0;

        while (current != null && depth < maxDepth) {
            // If we find the current accommodation in the parent chain, it's circular
            if (current.getId().equals(currentAccommodationId)) {
                log.warn("Circular reference detected: Accommodation {} found in parent chain of {}",
                    currentAccommodationId, proposedParent.getId());
                return true;
            }

            // Move up to the next parent
            current = current.getParentAccommodation();
            depth++;
        }

        if (depth >= maxDepth) {
            log.error("Maximum parent chain depth exceeded. Possible circular reference or very deep hierarchy.");
            return true; // Treat as circular to be safe
        }

        return false;
    }

    /**
     * Convert Accommodation entity to AccommodationDTO
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
}
