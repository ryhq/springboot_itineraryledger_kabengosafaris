package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.UpdateSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * SafariUpdateService - Service for updating Safari entities
 */
@Service
@Slf4j
public class SafariUpdateService {

    private final SafariRepository safariRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariUpdateService(
            SafariRepository safariRepository,
            UserRepository userRepository,
            IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.userRepository = userRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Update Safari basic fields
     */
    @Transactional
    @AuditLogAnnotation(action = "UPDATE_SAFARI", description = "Updating safari details", entityType = "Safari")
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

            // Get current user for audit tracking
            User currentUser = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String username = authentication.getName();
                currentUser = userRepository.findByUsername(username).orElse(null);
            }

            // Set updatedBy
            if (currentUser != null) {
                safari.setUpdatedBy(currentUser);
            }

            // Update fields if provided
            // Note: totalDays and totalNights are inherited from itinerary and cannot be updated
            if (dto.getName() != null && !dto.getName().isEmpty()) {
                safari.setName(dto.getName());
                safari.setSlug(generateSlug(dto.getName()));
            }

            if (dto.getStartDate() != null) {
                // Validate start date is not in the past
                if (dto.getStartDate().isBefore(LocalDate.now())) {
                    return ResponseEntity.badRequest().body(
                            ApiResponse.error(400,
                                    "Start date cannot be in the past. Provided: " + dto.getStartDate() + ", Today: " + LocalDate.now(),
                                    "START_DATE_IN_PAST")
                    );
                }

                LocalDate oldStartDate = safari.getStartDate();
                LocalDate newStartDate = dto.getStartDate();

                safari.setStartDate(newStartDate);
                // Recalculate end date based on totalDays from itinerary
                safari.setEndDate(newStartDate.plusDays(safari.getTotalDays() - 1));

                // Recalculate all safari day dates based on new start date
                if (safari.getDays() != null && !safari.getDays().isEmpty()) {
                    log.info("Recalculating safari day dates due to start date change from {} to {}", oldStartDate, newStartDate);
                    for (SafariDay day : safari.getDays()) {
                        // Calculate new actual date: new start date + (dayNumber - 1)
                        LocalDate newActualDate = newStartDate.plusDays(day.getDayNumber() - 1);
                        day.setActualDate(newActualDate);
                        log.debug("Updated day {} actual date to {}", day.getDayNumber(), newActualDate);
                    }
                }
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

        // Customer reference
        if (safari.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(safari.getCustomer().getId()));
            dto.setCustomerName(safari.getCustomer().getDisplayName());
            dto.setCustomerCode(safari.getCustomer().getCode());
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

        // Audit information
        if (safari.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(safari.getCreatedBy().getId()));
            dto.setCreatedByUsername(safari.getCreatedBy().getUsername());
            dto.setCreatedByFullName(safari.getCreatedBy().getUsername());
        }
        if (safari.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(safari.getUpdatedBy().getId()));
            dto.setUpdatedByUsername(safari.getUpdatedBy().getUsername());
            dto.setUpdatedByFullName(safari.getUpdatedBy().getUsername());
        }

        dto.setCreatedAt(safari.getCreatedAt());
        dto.setUpdatedAt(safari.getUpdatedAt());

        return dto;
    }
}
