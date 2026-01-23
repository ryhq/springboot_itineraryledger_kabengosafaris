package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.UpdateSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SafariUpdateService - Service for updating Safari entities
 */
@Service
@Slf4j
public class SafariUpdateService {

    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariUpdateService(
            SafariRepository safariRepository,
            IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update Safari basic fields
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateSafari(String idObfuscated, UpdateSafariDTO dto) {
        log.info("Updating safari with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode safari ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            Safari safari = safariRepository.findById(id).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Check if safari is editable
            if (!safari.isEditable()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Safari cannot be edited in state: " + safari.getState().getDisplayName(), "SAFARI_NOT_EDITABLE")
                );
            }

            // Update fields if provided
            // Note: totalDays and totalNights are inherited from itinerary and cannot be updated
            if (dto.getName() != null && !dto.getName().isEmpty()) {
                safari.setName(dto.getName());
                safari.setSlug(generateSlug(dto.getName()));
            }

            if (dto.getStartDate() != null) {
                safari.setStartDate(dto.getStartDate());
                // Recalculate end date based on totalDays from itinerary
                safari.setEndDate(dto.getStartDate().plusDays(safari.getTotalDays() - 1));
            }

            if (dto.getCarCount() != null) {
                safari.setCarCount(dto.getCarCount());
            }

            if (dto.getDescription() != null) {
                safari.setDescription(dto.getDescription());
            }

            if (dto.getHighlights() != null) {
                safari.setHighlights(dto.getHighlights());
            }

            if (dto.getStartLocation() != null) {
                safari.setStartLocation(dto.getStartLocation());
            }

            if (dto.getEndLocation() != null) {
                safari.setEndLocation(dto.getEndLocation());
            }

            if (dto.getSpecialRequests() != null) {
                safari.setSpecialRequests(dto.getSpecialRequests());
            }

            if (dto.getDietaryRequirements() != null) {
                safari.setDietaryRequirements(dto.getDietaryRequirements());
            }

            if (dto.getInternalNotes() != null) {
                safari.setInternalNotes(dto.getInternalNotes());
            }

            if (dto.getEmergencyContact() != null) {
                safari.setEmergencyContact(dto.getEmergencyContact());
            }

            Safari savedSafari = safariRepository.save(safari);
            log.info("Safari updated successfully: {}", savedSafari.getId());

            SafariDTO safariDTO = convertToDTO(savedSafari);

            return ResponseEntity.ok().body(
                    ApiResponse.success(200, "Safari updated successfully", safariDTO)
            );

        } catch (Exception e) {
            log.error("Error updating safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update safari", "SAFARI_UPDATE_FAILED")
            );
        }
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private SafariDTO convertToDTO(Safari safari) {
        SafariDTO dto = new SafariDTO();
        dto.setId(idObfuscator.encodeId(safari.getId()));
        dto.setName(safari.getName());
        dto.setCode(safari.getCode());
        dto.setSlug(safari.getSlug());

        if (safari.getItinerary() != null) {
            dto.setItineraryId(idObfuscator.encodeId(safari.getItinerary().getId()));
            dto.setItineraryName(safari.getItinerary().getName());
            dto.setItineraryCode(safari.getItinerary().getCode());
        }

        // State information (booking/operational)
        dto.setState(safari.getState());
        dto.setStateDisplayName(safari.getState().getDisplayName());
        dto.setStateDescription(safari.getState().getDescription());
        dto.setStateReason(safari.getStateReason());
        dto.setStateChangedAt(safari.getStateChangedAt());

        // Phase information (time-based)
        var phase = safari.getCurrentPhase();
        dto.setPhase(phase);
        dto.setPhaseDisplayName(phase.getDisplayName());
        dto.setPhaseDescription(phase.getDescription());
        dto.setPhaseUrgencyLevel(phase.getUrgencyLevel());
        dto.setPhaseColorCode(phase.getColorCode());

        dto.setStartDate(safari.getStartDate());
        dto.setEndDate(safari.getEndDate());

        dto.setTotalDays(safari.getTotalDays());
        dto.setTotalNights(safari.getTotalNights());
        dto.setCarCount(safari.getCarCount());

        dto.setDescription(safari.getDescription());
        dto.setHighlights(safari.getHighlights());
        dto.setStartLocation(safari.getStartLocation());
        dto.setEndLocation(safari.getEndLocation());

        dto.setSpecialRequests(safari.getSpecialRequests());
        dto.setDietaryRequirements(safari.getDietaryRequirements());
        dto.setEmergencyContact(safari.getEmergencyContact());

        dto.setIsActive(safari.getIsActive());
        dto.setIsEditable(safari.isEditable());
        dto.setIsCancellable(safari.isCancellable());
        dto.setHasStarted(safari.hasStarted());
        dto.setHasEnded(safari.hasEnded());
        dto.setIsInProgress(safari.isInProgress());
        dto.setIsUrgentPhase(safari.isUrgentPhase());

        // Time calculations
        dto.setDaysUntilStart(safari.getDaysUntilStart());
        dto.setDaysSinceEnd(safari.getDaysSinceEnd());
        dto.setCurrentDayNumber(safari.getCurrentDayNumber());

        dto.setTotalPaxCount(safari.getTotalPaxCount());
        dto.setTotalDaysCount(safari.getDays() != null ? safari.getDays().size() : 0);

        dto.setCreatedAt(safari.getCreatedAt());
        dto.setUpdatedAt(safari.getUpdatedAt());

        return dto;
    }
}
