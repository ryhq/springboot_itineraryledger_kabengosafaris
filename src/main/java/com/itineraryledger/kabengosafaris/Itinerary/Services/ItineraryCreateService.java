package com.itineraryledger.kabengosafaris.Itinerary.Services;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Repository.InclusionItemRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.CreateItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryCreateService - Service for creating itineraries
 */
@Service
@Slf4j
@Transactional
public class ItineraryCreateService {

    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;
    private final InclusionItemRepository inclusionItems;

    @Autowired
    public ItineraryCreateService(
        ItineraryRepository itineraryRepository,
        IdObfuscator idObfuscator,
        InclusionItemRepository inclusionItems
    ) {
        this.itineraryRepository = itineraryRepository;
        this.idObfuscator = idObfuscator;
        this.inclusionItems = inclusionItems;
    }

    /**
     * Create a new itinerary
     *
     * @param createItineraryDTO The itinerary data
     * @return ResponseEntity with ApiResponse containing the created itinerary
     */
    @AuditLogAnnotation(action = "CREATE_ITINERARY", description = "Creating a new itinerary", entityType = "Itinerary")
    public ResponseEntity<ApiResponse<?>> createItinerary(CreateItineraryDTO createItineraryDTO) {
        log.info("Creating new itinerary: {}", createItineraryDTO.getName());

        try {
            /*
             * What the price covers moved out of these two columns and into rows of its own. A
             * caller still sending the text is told where it went rather than having it quietly
             * written to a column nothing reads any more.
             */
            if (createItineraryDTO.getInclusions() != null || createItineraryDTO.getExclusions() != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "What a trip's price covers is now a list of catalogue lines. Create the "
                    + "itinerary without these fields, then set them with "
                    + "PUT /api/itineraries/{id}/inclusions.",
                    "INCLUSIONS_MOVED"));
            }

            // Validate name uniqueness
            if (itineraryRepository.existsByNameIgnoreCase(createItineraryDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Itinerary with name '" + createItineraryDTO.getName() + "' already exists",
                        "ITINERARY_NAME_EXISTS"
                    )
                );
            }

            // Calculate total nights if not provided
            Integer totalNights = createItineraryDTO.getTotalNights();
            if (totalNights == null) {
                totalNights = Math.max(0, createItineraryDTO.getTotalDays() - 1);
            }

            // Create itinerary entity
            Itinerary itinerary = Itinerary.builder()
                .name(createItineraryDTO.getName())
                .tripType(createItineraryDTO.getTripType())
                .budgetCategory(createItineraryDTO.getBudgetCategory())
                .totalDays(createItineraryDTO.getTotalDays())
                .totalNights(totalNights)
                .carCount(createItineraryDTO.getCarCount() != null ? createItineraryDTO.getCarCount() : 1)
                .description(createItineraryDTO.getDescription())
                .highlights(createItineraryDTO.getHighlights())
                .startLocation(createItineraryDTO.getStartLocation())
                .endLocation(createItineraryDTO.getEndLocation())
                .build();

            // Save itinerary (first save to get ID)
            itinerary = itineraryRepository.save(itinerary);

            // Generate and set code after saving (requires ID)
            String code = itinerary.generateCode();
            itinerary.setCode(code);

            /*
             * A new trip starts with the house promise, so nobody has to remember to add it — the
             * three itineraries that showed no "what's included" section on the website were
             * simply ones nobody had pasted it into. Anything marked as not standard is left out,
             * so a trip-specific line never appears on paperwork it has nothing to do with.
             */
            int position = 1;
            for (InclusionItem item : inclusionItems.findByIsActiveTrueAndIsStandardTrueOrderByDisplayOrderAscIdAsc()) {
                itinerary.addInclusion(ItineraryInclusion.builder()
                    .inclusionItem(item)
                    .isIncluded(!Boolean.FALSE.equals(item.getDefaultIncluded()))
                    .sortOrder(position++)
                    .build());
            }

            itinerary = itineraryRepository.save(itinerary);

            // Convert to DTO
            ItineraryDTO itineraryDTO = convertToDTO(itinerary);

            log.info("Itinerary created successfully: {} (code: {})", itinerary.getName(), itinerary.getCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                    201,
                    "Itinerary created successfully",
                    itineraryDTO
                )
            );

        } catch (Exception e) {
            log.error("Error creating itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to create itinerary",
                    "ITINERARY_CREATE_FAILED"
                )
            );
        }
    }

    /**
     * Convert Itinerary entity to ItineraryDTO
     */
    private ItineraryDTO convertToDTO(Itinerary itinerary) {
        ItineraryDTO dto = new ItineraryDTO();
        dto.setId(idObfuscator.encodeId(itinerary.getId()));
        dto.setName(itinerary.getName());
        dto.setCode(itinerary.getCode());
        dto.setStatus(itinerary.getStatus());
        dto.setStatusDisplayName(itinerary.getStatus().getDisplayName());
        dto.setTripType(itinerary.getTripType());
        dto.setTripTypeDisplayName(itinerary.getTripType() != null ? itinerary.getTripType().getDisplayName() : null);
        dto.setTripTypeDescription(itinerary.getTripType() != null ? itinerary.getTripType().getDescription() : null);
        dto.setBudgetCategory(itinerary.getBudgetCategory());
        dto.setBudgetCategoryDisplayName(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDisplayName() : null);
        dto.setBudgetCategoryDescription(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDescription() : null);
        dto.setBudgetCategoryTier(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getTier() : null);
        dto.setTotalDays(itinerary.getTotalDays());
        dto.setTotalNights(itinerary.getTotalNights());
        dto.setIsDayTrip(itinerary.getTotalDays() == 1 && itinerary.getTotalNights() == 0);
        dto.setCarCount(itinerary.getCarCount());
        dto.setDescription(itinerary.getDescription());
        dto.setHighlights(itinerary.getHighlights());
        dto.setInclusions(itinerary.getInclusions());
        dto.setExclusions(itinerary.getExclusions());
        dto.setStartLocation(itinerary.getStartLocation());
        dto.setEndLocation(itinerary.getEndLocation());
        dto.setIsActive(itinerary.getIsActive());
        dto.setTotalPaxCount(itinerary.getTotalPaxCount());
        dto.setTotalDaysCount(itinerary.getDays() != null ? itinerary.getDays().size() : 0);
        dto.setCreatedAt(itinerary.getCreatedAt());
        dto.setUpdatedAt(itinerary.getUpdatedAt());
        return dto;
    }
}
