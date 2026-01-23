package com.itineraryledger.kabengosafaris.Safari.Services;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.DTOs.CreateSafariFromItineraryDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDTO;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * SafariCreateService - Service for creating Safari bookings from Itinerary templates
 *
 * This service performs a deep copy of an Itinerary structure to create a new Safari.
 * The Safari will have actual dates calculated from the startDate.
 */
@Service
@Slf4j
public class SafariCreateService {

    private final SafariRepository safariRepository;
    private final ItineraryRepository itineraryRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariCreateService(
            SafariRepository safariRepository,
            ItineraryRepository itineraryRepository,
            IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.itineraryRepository = itineraryRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new Safari from an Itinerary template
     *
     * @param dto The creation request with itinerary ID and start date
     * @return ResponseEntity with ApiResponse containing the created Safari
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> createSafariFromItinerary(CreateSafariFromItineraryDTO dto) {
        log.info("Creating Safari from Itinerary: {}", dto.getItineraryId());

        try {
            // Decode and validate itinerary ID
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(dto.getItineraryId());
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", dto.getItineraryId(), e);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // Find the source itinerary
            Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Validate itinerary status - only PUBLISHED itineraries can be used
            if (itinerary.getStatus() != Itinerary.ItineraryStatus.PUBLISHED) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Cannot create safari from itinerary with status: " + itinerary.getStatus().getDisplayName() +
                                        ". Only published itineraries can be used.",
                                "ITINERARY_NOT_PUBLISHED")
                );
            }

            // Validate start date
            if (dto.getStartDate() == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Start date is required", "START_DATE_REQUIRED")
                );
            }

            // Calculate end date based on itinerary totalDays
            LocalDate endDate = dto.getStartDate().plusDays(itinerary.getTotalDays() - 1);

            // Create the Safari entity (code will be generated after save)
            Safari safari = Safari.builder()
                    .itinerary(itinerary)
                    .name(dto.getName() != null ? dto.getName() : itinerary.getName())
                    .slug(generateSlug(dto.getName() != null ? dto.getName() : itinerary.getName()))
                    .startDate(dto.getStartDate())
                    .endDate(endDate)
                    .state(SafariState.DRAFT)
                    .totalDays(itinerary.getTotalDays())
                    .totalNights(itinerary.getTotalNights())
                    .carCount(itinerary.getCarCount())
                    .description(dto.getDescription() != null ? dto.getDescription() : itinerary.getDescription())
                    .highlights(itinerary.getHighlights())
                    .startLocation(itinerary.getStartLocation())
                    .endLocation(itinerary.getEndLocation())
                    .specialRequests(dto.getSpecialRequests())
                    .dietaryRequirements(dto.getDietaryRequirements())
                    .emergencyContact(dto.getEmergencyContact())
                    .isActive(true)
                    .build();

            // Deep copy pax configuration
            copyPaxConfiguration(itinerary, safari);

            // Deep copy days with all nested entities
            copyDaysStructure(itinerary, safari, dto.getStartDate());

            // Save the Safari to get the ID (cascade will save all nested entities)
            Safari savedSafari = safariRepository.save(safari);

            // Generate and set the code using entity method (requires ID)
            savedSafari.setCode(savedSafari.generateCode());
            savedSafari = safariRepository.save(savedSafari);

            log.info("Safari created successfully with ID: {} and code: {}", savedSafari.getId(), savedSafari.getCode());

            // Convert to DTO and return
            SafariDTO safariDTO = convertToDTO(savedSafari);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Safari created successfully", safariDTO)
            );

        } catch (Exception e) {
            log.error("Error creating Safari from Itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create Safari: " + e.getMessage(), "SAFARI_CREATE_FAILED")
            );
        }
    }

    /**
     * Generate URL-friendly slug from name
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    /**
     * Deep copy pax configuration from Itinerary to Safari
     */
    private void copyPaxConfiguration(Itinerary itinerary, Safari safari) {
        if (itinerary.getPaxList() == null || itinerary.getPaxList().isEmpty()) {
            return;
        }

        for (ItineraryPax itineraryPax : itinerary.getPaxList()) {
            SafariPax safariPax = SafariPax.builder()
                    .nationCategory(itineraryPax.getNationCategory())
                    .ageCategory(itineraryPax.getAgeCategory())
                    .count(itineraryPax.getCount())
                    .notes(itineraryPax.getNotes())
                    .build();

            safari.addPax(safariPax);
        }
    }

    /**
     * Deep copy day structure from Itinerary to Safari
     */
    private void copyDaysStructure(Itinerary itinerary, Safari safari, LocalDate startDate) {
        if (itinerary.getDays() == null || itinerary.getDays().isEmpty()) {
            return;
        }

        for (ItineraryDay itineraryDay : itinerary.getDays()) {
            // Calculate actual date for this day
            LocalDate actualDate = startDate.plusDays(itineraryDay.getDayNumber() - 1);

            SafariDay safariDay = SafariDay.builder()
                    .dayNumber(itineraryDay.getDayNumber())
                    .dayTag(itineraryDay.getDayTag())
                    .title(itineraryDay.getTitle())
                    .actualDate(actualDate)
                    .description(itineraryDay.getDescription())
                    .morningActivities(itineraryDay.getMorningActivities())
                    .afternoonActivities(itineraryDay.getAfternoonActivities())
                    .eveningActivities(itineraryDay.getEveningActivities())
                    .wildlifeHighlights(itineraryDay.getWildlifeHighlights())
                    .scenicHighlights(itineraryDay.getScenicHighlights())
                    .specialNotes(itineraryDay.getSpecialNotes())
                    .startLocation(itineraryDay.getStartLocation())
                    .endLocation(itineraryDay.getEndLocation())
                    .distanceKm(itineraryDay.getDistanceKm())
                    .isOvernight(itineraryDay.getIsOvernight())
                    .mealsIncluded(itineraryDay.getMealsIncluded())
                    .internalNotes(itineraryDay.getInternalNotes())
                    .isModified(false)
                    .build();

            // Copy activities
            copyDayActivities(itineraryDay, safariDay);

            // Copy accommodations
            copyDayAccommodations(itineraryDay, safariDay);

            // Copy parks (with nested activities and tariffs)
            copyDayParks(itineraryDay, safariDay);

            safari.addDay(safariDay);
        }
    }

    /**
     * Copy day activities from ItineraryDay to SafariDay
     */
    private void copyDayActivities(ItineraryDay itineraryDay, SafariDay safariDay) {
        if (itineraryDay.getActivities() == null || itineraryDay.getActivities().isEmpty()) {
            return;
        }

        for (ItineraryDayActivity itineraryActivity : itineraryDay.getActivities()) {
            SafariDayActivity safariActivity = SafariDayActivity.builder()
                    .activity(itineraryActivity.getActivity())
                    .sortOrder(itineraryActivity.getSortOrder())
                    .durationHours(itineraryActivity.getDurationHours())
                    .startTime(itineraryActivity.getStartTime())
                    .endTime(itineraryActivity.getEndTime())
                    .notes(itineraryActivity.getNotes())
                    .isIncludedInPrice(itineraryActivity.getIsIncludedInPrice())
                    .isOptional(itineraryActivity.getIsOptional())
                    .isCompleted(false)
                    .isSkipped(false)
                    .build();

            safariDay.addActivity(safariActivity);
        }
    }

    /**
     * Copy day accommodations from ItineraryDay to SafariDay
     */
    private void copyDayAccommodations(ItineraryDay itineraryDay, SafariDay safariDay) {
        if (itineraryDay.getAccommodations() == null || itineraryDay.getAccommodations().isEmpty()) {
            return;
        }

        for (ItineraryDayAccommodation itineraryAccommodation : itineraryDay.getAccommodations()) {
            SafariDayAccommodation safariAccommodation = SafariDayAccommodation.builder()
                    .accommodation(itineraryAccommodation.getAccommodation())
                    .roomType(itineraryAccommodation.getRoomType())
                    .roomStandard(itineraryAccommodation.getRoomStandard())
                    .boardType(itineraryAccommodation.getBoardType())
                    .roomCount(itineraryAccommodation.getRoomCount())
                    .isAlternative(itineraryAccommodation.getIsAlternative())
                    .notes(itineraryAccommodation.getNotes())
                    .bookingStatus(SafariDayAccommodation.BookingStatus.PENDING)
                    .build();

            safariDay.addAccommodation(safariAccommodation);
        }
    }

    /**
     * Copy day parks from ItineraryDay to SafariDay (including nested activities and tariffs)
     */
    private void copyDayParks(ItineraryDay itineraryDay, SafariDay safariDay) {
        if (itineraryDay.getParks() == null || itineraryDay.getParks().isEmpty()) {
            return;
        }

        for (ItineraryDayPark itineraryPark : itineraryDay.getParks()) {
            SafariDayPark safariPark = SafariDayPark.builder()
                    .park(itineraryPark.getPark())
                    .entryType(itineraryPark.getEntryType())
                    .sortOrder(itineraryPark.getSortOrder())
                    .arrivalTime(itineraryPark.getArrivalTime())
                    .departureTime(itineraryPark.getDepartureTime())
                    .notes(itineraryPark.getNotes())
                    .feesPaid(false)
                    .build();

            // Copy park activities
            copyParkActivities(itineraryPark, safariPark);

            // Copy park tariffs
            copyParkTariffs(itineraryPark, safariPark);

            safariDay.addPark(safariPark);
        }
    }

    /**
     * Copy park activities from ItineraryDayPark to SafariDayPark
     */
    private void copyParkActivities(ItineraryDayPark itineraryPark, SafariDayPark safariPark) {
        if (itineraryPark.getParkActivities() == null || itineraryPark.getParkActivities().isEmpty()) {
            return;
        }

        for (ItineraryDayParkActivity itineraryParkActivity : itineraryPark.getParkActivities()) {
            SafariDayParkActivity safariParkActivity = SafariDayParkActivity.builder()
                    .parkActivity(itineraryParkActivity.getParkActivity())
                    .sortOrder(itineraryParkActivity.getSortOrder())
                    .durationHours(itineraryParkActivity.getDurationHours())
                    .notes(itineraryParkActivity.getNotes())
                    .isIncludedInPrice(itineraryParkActivity.getIsIncludedInPrice())
                    .isCompleted(false)
                    .isSkipped(false)
                    .build();

            safariPark.addParkActivity(safariParkActivity);
        }
    }

    /**
     * Copy park tariffs from ItineraryDayPark to SafariDayPark
     */
    private void copyParkTariffs(ItineraryDayPark itineraryPark, SafariDayPark safariPark) {
        if (itineraryPark.getParkTariffs() == null || itineraryPark.getParkTariffs().isEmpty()) {
            return;
        }

        for (ItineraryDayParkTariff itineraryParkTariff : itineraryPark.getParkTariffs()) {
            SafariDayParkTariff safariParkTariff = SafariDayParkTariff.builder()
                    .parkTariff(itineraryParkTariff.getParkTariff())
                    .notes(itineraryParkTariff.getNotes())
                    .isIncludedInPrice(itineraryParkTariff.getIsIncludedInPrice())
                    .isPaid(false)
                    .isWaived(false)
                    .build();

            safariPark.addParkTariff(safariParkTariff);
        }
    }

    /**
     * Convert Safari entity to SafariDTO
     */
    private SafariDTO convertToDTO(Safari safari) {
        SafariDTO dto = new SafariDTO();
        dto.setId(idObfuscator.encodeId(safari.getId()));
        dto.setName(safari.getName());
        dto.setCode(safari.getCode());
        dto.setSlug(safari.getSlug());

        // Itinerary reference
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

        // Dates
        dto.setStartDate(safari.getStartDate());
        dto.setEndDate(safari.getEndDate());

        // Duration
        dto.setTotalDays(safari.getTotalDays());
        dto.setTotalNights(safari.getTotalNights());
        dto.setCarCount(safari.getCarCount());

        // Descriptions
        dto.setDescription(safari.getDescription());
        dto.setHighlights(safari.getHighlights());
        dto.setStartLocation(safari.getStartLocation());
        dto.setEndLocation(safari.getEndLocation());

        // Safari-specific
        dto.setSpecialRequests(safari.getSpecialRequests());
        dto.setDietaryRequirements(safari.getDietaryRequirements());
        dto.setEmergencyContact(safari.getEmergencyContact());

        // Status flags
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

        // Counts
        dto.setTotalPaxCount(safari.getTotalPaxCount());
        dto.setTotalDaysCount(safari.getDays() != null ? safari.getDays().size() : 0);

        // Metadata
        dto.setCreatedAt(safari.getCreatedAt());
        dto.setUpdatedAt(safari.getUpdatedAt());

        return dto;
    }
}
