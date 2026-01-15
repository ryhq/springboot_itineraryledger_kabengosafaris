package com.itineraryledger.kabengosafaris.Itinerary.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.UpdateItineraryDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryUpdateService - Service for updating itineraries
 */
@Service
@Slf4j
@Transactional
public class ItineraryUpdateService {

    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ItineraryUpdateService(
        ItineraryRepository itineraryRepository,
        IdObfuscator idObfuscator
    ) {
        this.itineraryRepository = itineraryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update an itinerary by obfuscated ID
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @param updateItineraryDTO The updated itinerary data
     * @return ResponseEntity with ApiResponse containing the updated itinerary
     */
    @AuditLogAnnotation(action = "UPDATE_ITINERARY", description = "Updating itinerary", entityType = "Itinerary", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateItinerary(String idObfuscated, UpdateItineraryDTO updateItineraryDTO) {
        log.info("Updating itinerary with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Invalid itinerary ID",
                        "INVALID_ITINERARY_ID"
                    )
                );
            }

            return updateItineraryById(id, updateItineraryDTO);

        } catch (Exception e) {
            log.error("Error updating itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to update itinerary",
                    "ITINERARY_UPDATE_FAILED"
                )
            );
        }
    }

    /**
     * Update an itinerary by ID (internal method)
     */
    private ResponseEntity<ApiResponse<?>> updateItineraryById(Long id, UpdateItineraryDTO updateItineraryDTO) {
        // Find itinerary
        Itinerary itinerary = itineraryRepository.findById(id).orElse(null);
        if (itinerary == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(
                    404,
                    "Itinerary not found",
                    "ITINERARY_NOT_FOUND"
                )
            );
        }

        // Check if name is being changed and if it's unique
        if (updateItineraryDTO.getName() != null && !updateItineraryDTO.getName().equals(itinerary.getName())) {
            if (itineraryRepository.existsByNameIgnoreCaseAndIdNot(updateItineraryDTO.getName(), id)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        400,
                        "Itinerary with name '" + updateItineraryDTO.getName() + "' already exists",
                        "ITINERARY_NAME_EXISTS"
                    )
                );
            }
            itinerary.setName(updateItineraryDTO.getName());
        }

        // Update other fields if provided
        boolean daysOrNightsChanged = false;
        if (updateItineraryDTO.getTotalDays() != null) {
            itinerary.setTotalDays(updateItineraryDTO.getTotalDays());
            daysOrNightsChanged = true;
        }
        if (updateItineraryDTO.getTotalNights() != null) {
            itinerary.setTotalNights(updateItineraryDTO.getTotalNights());
            daysOrNightsChanged = true;
        }
        if (updateItineraryDTO.getCarCount() != null) {
            itinerary.setCarCount(updateItineraryDTO.getCarCount());
        }
        if (updateItineraryDTO.getDescription() != null) {
            itinerary.setDescription(updateItineraryDTO.getDescription());
        }
        if (updateItineraryDTO.getHighlights() != null) {
            itinerary.setHighlights(updateItineraryDTO.getHighlights());
        }
        if (updateItineraryDTO.getStartLocation() != null) {
            itinerary.setStartLocation(updateItineraryDTO.getStartLocation());
        }
        if (updateItineraryDTO.getEndLocation() != null) {
            itinerary.setEndLocation(updateItineraryDTO.getEndLocation());
        }
        if (updateItineraryDTO.getIsActive() != null) {
            itinerary.setIsActive(updateItineraryDTO.getIsActive());
        }
        if (updateItineraryDTO.getTripType() != null) {
            itinerary.setTripType(updateItineraryDTO.getTripType());
        }
        if (updateItineraryDTO.getBudgetCategory() != null) {
            itinerary.setBudgetCategory(updateItineraryDTO.getBudgetCategory());
        }

        // Regenerate code if days or nights changed
        if (daysOrNightsChanged) {
            String newCode = itinerary.generateCode();
            itinerary.setCode(newCode);
        }

        // Save updated itinerary
        itinerary = itineraryRepository.save(itinerary);

        // Convert to DTO
        ItineraryDTO itineraryDTO = convertToDTO(itinerary);

        log.info("Itinerary updated successfully: {}", itinerary.getName());

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                "Itinerary updated successfully",
                itineraryDTO
            )
        );
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
