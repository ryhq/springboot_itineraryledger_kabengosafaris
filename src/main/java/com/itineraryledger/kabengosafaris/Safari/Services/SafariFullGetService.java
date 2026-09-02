package com.itineraryledger.kabengosafaris.Safari.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO.*;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository.SafariDayRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Repository.SafariDayActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository.SafariDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository.SafariDayParkRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Repository.SafariDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Repository.SafariDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository.SafariPaxRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * SafariFullGetService - Service for retrieving complete safari with all nested data
 *
 * Returns the full safari structure including:
 * - Safari base data
 * - Pax configurations
 * - Days (ordered by dayNumber)
 *   - Day activities (ordered by sortOrder)
 *   - Day accommodations
 *   - Day parks (ordered by sortOrder)
 *     - Park activities (ordered by sortOrder)
 *     - Park tariffs
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class SafariFullGetService {

    private final SafariRepository safariRepository;
    private final SafariDayRepository dayRepository;
    private final SafariPaxRepository paxRepository;
    private final SafariDayActivityRepository dayActivityRepository;
    private final SafariDayAccommodationRepository dayAccommodationRepository;
    private final SafariDayParkRepository dayParkRepository;
    private final SafariDayParkActivityRepository parkActivityRepository;
    private final SafariDayParkTariffRepository parkTariffRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public SafariFullGetService(
        SafariRepository safariRepository,
        SafariDayRepository dayRepository,
        SafariPaxRepository paxRepository,
        SafariDayActivityRepository dayActivityRepository,
        SafariDayAccommodationRepository dayAccommodationRepository,
        SafariDayParkRepository dayParkRepository,
        SafariDayParkActivityRepository parkActivityRepository,
        SafariDayParkTariffRepository parkTariffRepository,
        IdObfuscator idObfuscator
    ) {
        this.safariRepository = safariRepository;
        this.dayRepository = dayRepository;
        this.paxRepository = paxRepository;
        this.dayActivityRepository = dayActivityRepository;
        this.dayAccommodationRepository = dayAccommodationRepository;
        this.dayParkRepository = dayParkRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.parkTariffRepository = parkTariffRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get complete safari with all nested data by obfuscated ID
     *
     * @param idObfuscated The obfuscated safari ID
     * @return ResponseEntity with ApiResponse containing the full safari
     */
    public ResponseEntity<ApiResponse<?>> getFullSafari(String idObfuscated) {
        log.info("Fetching full safari with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode safari ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // Find safari
            Safari safari = safariRepository.findById(id).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Build full DTO
            FullSafariDTO fullDTO = buildFullSafariDTO(safari);

            log.info("Full safari retrieved successfully: {} with {} days, {} pax configurations",
                safari.getName(),
                fullDTO.getTotalDaysCount(),
                fullDTO.getTotalPaxCount());

            // Build navigation
            Long nextId = safariRepository.findNextId(id).orElse(null);
            Long previousId = safariRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = safariRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = safariRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("safari", fullDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Full safari retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching full safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch full safari", "FULL_SAFARI_FETCH_FAILED")
            );
        }
    }

    /**
     * Build the complete FullSafariDTO with all nested data
     */
    private FullSafariDTO buildFullSafariDTO(Safari safari) {
        FullSafariDTO dto = new FullSafariDTO();

        // ========================
        // SAFARI BASE FIELDS
        // ========================
        dto.setId(idObfuscator.encodeId(safari.getId()));
        dto.setCustomer(safari.getCustomer() != null ? buildCustomerDTO(safari.getCustomer()) : null);
        dto.setItinerary(safari.getItinerary() != null ? buildItineraryDTO(safari.getItinerary()) : null);
        dto.setName(safari.getName());
        dto.setCode(safari.getCode());
        dto.setSlug(safari.getSlug());
        dto.setState(safari.getState());
        dto.setStateDisplayName(safari.getState().getDisplayName());
        dto.setStateReason(safari.getStateReason());
        dto.setStateChangedAt(safari.getStateChangedAt());
        dto.setCurrentPhase(safari.getCurrentPhase());
        dto.setCurrentPhaseDisplayName(safari.getCurrentPhase().getDisplayName());
        dto.setIsUrgentPhase(safari.isUrgentPhase());
        dto.setStartDate(safari.getStartDate());
        dto.setEndDate(safari.getEndDate());
        dto.setTotalDays(safari.getTotalDays());
        dto.setTotalNights(safari.getTotalNights());
        dto.setIsDayTrip(safari.getTotalDays() == 1 && safari.getTotalNights() == 0);
        dto.setCarCount(safari.getCarCount());
        dto.setDescription(safari.getDescription());
        dto.setHighlights(safari.getHighlights());
        dto.setStartLocation(safari.getStartLocation());
        dto.setEndLocation(safari.getEndLocation());
        dto.setSpecialRequests(safari.getSpecialRequests());
        dto.setDietaryRequirements(safari.getDietaryRequirements());
        dto.setInternalNotes(safari.getInternalNotes());
        dto.setEmergencyContact(safari.getEmergencyContact());
        dto.setIsActive(safari.getIsActive());
        dto.setCurrentDayNumber(safari.getCurrentDayNumber());
        dto.setDaysUntilStart(safari.getDaysUntilStart());
        dto.setDaysSinceEnd(safari.getDaysSinceEnd());
        dto.setHasStarted(safari.hasStarted());
        dto.setHasEnded(safari.hasEnded());
        dto.setIsInProgress(safari.isInProgress());
        dto.setIsEditable(safari.isEditable());
        dto.setIsCancellable(safari.isCancellable());
        dto.setCreatedByName(safari.getCreatedBy() != null ? getUserDisplayName(safari.getCreatedBy()) : null);
        dto.setUpdatedByName(safari.getUpdatedBy() != null ? getUserDisplayName(safari.getUpdatedBy()) : null);
        dto.setCreatedAt(safari.getCreatedAt());
        dto.setUpdatedAt(safari.getUpdatedAt());

        // ========================
        // PAX LIST
        // ========================
        List<SafariPax> paxList = paxRepository.findBySafariId(safari.getId());
        List<PaxDTO> paxDTOs = paxList.stream()
            .map(this::convertPaxToDTO)
            .collect(Collectors.toList());
        dto.setPaxList(paxDTOs);

        // ========================
        // DAYS WITH NESTED DATA
        // ========================
        List<SafariDay> days = dayRepository.findBySafariIdOrderByDayNumberAsc(safari.getId());
        List<DayDTO> dayDTOs = new ArrayList<>();

        int totalParksCount = 0;
        int totalActivitiesCount = 0;
        int totalAccommodationsCount = 0;

        for (SafariDay day : days) {
            DayDTO dayDTO = buildDayDTO(day);
            dayDTOs.add(dayDTO);

            // Count nested items
            if (dayDTO.getActivities() != null) {
                totalActivitiesCount += dayDTO.getActivities().size();
            }
            if (dayDTO.getAccommodations() != null) {
                totalAccommodationsCount += dayDTO.getAccommodations().size();
            }
            if (dayDTO.getParks() != null) {
                totalParksCount += dayDTO.getParks().size();
            }
        }

        dto.setDays(dayDTOs);

        // ========================
        // SUMMARY STATISTICS
        // ========================
        dto.setTotalPaxCount(safari.getTotalPaxCount());
        dto.setTotalDaysCount(days.size());
        dto.setTotalParksCount(totalParksCount);
        dto.setTotalActivitiesCount(totalActivitiesCount);
        dto.setTotalAccommodationsCount(totalAccommodationsCount);

        return dto;
    }

    /**
     * Build DayDTO with all nested data for a safari day
     */
    private DayDTO buildDayDTO(SafariDay day) {
        DayDTO dto = new DayDTO();

        // Base fields
        dto.setId(idObfuscator.encodeId(day.getId()));
        dto.setDayNumber(day.getDayNumber());
        dto.setDate(day.getActualDate());
        dto.setDayTag(day.getDayTag());
        dto.setTitle(day.getTitle());
        dto.setDescription(day.getDescription());
        dto.setMorningActivities(day.getMorningActivities());
        dto.setAfternoonActivities(day.getAfternoonActivities());
        dto.setEveningActivities(day.getEveningActivities());
        dto.setWildlifeHighlights(day.getWildlifeHighlights());
        dto.setScenicHighlights(day.getScenicHighlights());
        dto.setSpecialNotes(day.getSpecialNotes());
        dto.setStartLocation(day.getStartLocation());
        dto.setEndLocation(day.getEndLocation());
        dto.setDistanceKm(day.getDistanceKm());
        dto.setIsOvernight(day.getIsOvernight());
        dto.setMealsIncluded(day.getMealsIncluded());

        // Safari-specific fields
        dto.setActualStartTime(day.getActualStartTime());
        dto.setActualEndTime(day.getActualEndTime());
        dto.setWeatherConditions(day.getWeatherNotes());
        dto.setDayNotes(day.getDriverNotes());
        dto.setHighlightsOfDay(null); // Not tracked at entity level

        dto.setCreatedAt(day.getCreatedAt());

        // ========================
        // NESTED: DAY ACTIVITIES
        // ========================
        List<SafariDayActivity> activities = dayActivityRepository.findBySafariDayIdOrderBySortOrderAsc(day.getId());
        List<DayActivityDTO> activityDTOs = activities.stream()
            .map(this::convertDayActivityToDTO)
            .collect(Collectors.toList());
        dto.setActivities(activityDTOs);

        // ========================
        // NESTED: DAY ACCOMMODATIONS
        // ========================
        List<SafariDayAccommodation> accommodations = dayAccommodationRepository.findBySafariDayId(day.getId());
        List<DayAccommodationDTO> accommodationDTOs = accommodations.stream()
            .map(this::convertDayAccommodationToDTO)
            .collect(Collectors.toList());
        dto.setAccommodations(accommodationDTOs);

        // ========================
        // NESTED: DAY PARKS (with activities & tariffs)
        // ========================
        List<SafariDayPark> parks = dayParkRepository.findBySafariDayIdOrderBySortOrderAsc(day.getId());
        List<DayParkDTO> parkDTOs = parks.stream()
            .map(this::buildDayParkDTO)
            .collect(Collectors.toList());
        dto.setParks(parkDTOs);

        return dto;
    }

    /**
     * Build DayParkDTO with nested activities and tariffs
     */
    private DayParkDTO buildDayParkDTO(SafariDayPark park) {
        DayParkDTO dto = new DayParkDTO();

        // Base fields
        dto.setId(idObfuscator.encodeId(park.getId()));
        dto.setParkId(idObfuscator.encodeId(park.getPark().getId()));
        dto.setParkName(park.getPark().getName());
        dto.setParkSlug(park.getPark().getSlug());
        dto.setEntryType(park.getEntryType());
        dto.setEntryTypeDisplayName(park.getEntryType() != null ? park.getEntryType().getDisplayName() : null);
        dto.setSortOrder(park.getSortOrder());
        dto.setArrivalTime(park.getArrivalTime());
        dto.setDepartureTime(park.getDepartureTime());
        dto.setNotes(park.getNotes());

        // Safari-specific fields
        dto.setActualArrivalTime(park.getActualArrivalTime());
        dto.setActualDepartureTime(park.getActualDepartureTime());
        dto.setEntryReceiptNumber(park.getEntryReceiptNumber());
        dto.setWildlifeSightings(park.getWildlifeSightings());
        dto.setVisitNotes(park.getVisitNotes());
        dto.setFeesPaid(park.getFeesPaid());
        dto.setFeesPaidAt(park.getFeesPaidAt());
        dto.setWeatherConditions(park.getWeatherConditions());

        // ========================
        // NESTED: PARK ACTIVITIES
        // ========================
        List<SafariDayParkActivity> parkActivities = parkActivityRepository.findBySafariDayParkIdOrderBySortOrderAsc(park.getId());
        List<ParkActivityDTO> parkActivityDTOs = parkActivities.stream()
            .map(this::convertParkActivityToDTO)
            .collect(Collectors.toList());
        dto.setActivities(parkActivityDTOs);

        // ========================
        // NESTED: PARK TARIFFS
        // ========================
        List<SafariDayParkTariff> parkTariffs = parkTariffRepository.findBySafariDayParkId(park.getId());
        List<ParkTariffDTO> parkTariffDTOs = parkTariffs.stream()
            .map(this::convertParkTariffToDTO)
            .collect(Collectors.toList());
        dto.setTariffs(parkTariffDTOs);

        return dto;
    }

    /**
     * Convert SafariPax to PaxDTO
     */
    private PaxDTO convertPaxToDTO(SafariPax entity) {
        PaxDTO dto = new PaxDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setNationCategoryId(idObfuscator.encodeId(entity.getNationCategory().getId()));
        dto.setNationCategoryName(entity.getNationCategory().getName());
        dto.setAgeCategoryId(idObfuscator.encodeId(entity.getAgeCategory().getId()));
        dto.setAgeCategoryName(entity.getAgeCategory().getName());
        dto.setCount(entity.getCount());
        dto.setNotes(entity.getNotes());
        return dto;
    }

    /**
     * Convert SafariDayActivity to DayActivityDTO
     */
    private DayActivityDTO convertDayActivityToDTO(SafariDayActivity entity) {
        DayActivityDTO dto = new DayActivityDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setActivityId(idObfuscator.encodeId(entity.getActivity().getId()));
        dto.setActivityName(entity.getActivity().getName());
        dto.setActivitySlug(entity.getActivity().getSlug());
        dto.setSortOrder(entity.getSortOrder());
        dto.setDurationHours(entity.getDurationHours());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setNotes(entity.getNotes());
        dto.setIsIncludedInPrice(entity.getIsIncludedInPrice());
        dto.setIsOptional(entity.getIsOptional());

        // Safari-specific fields
        dto.setWasCompleted(entity.getIsCompleted());
        dto.setActualStartTime(entity.getActualStartTime());
        dto.setActualEndTime(entity.getActualEndTime());
        dto.setCompletionNotes(entity.getFeedback());

        return dto;
    }

    /**
     * Convert SafariDayAccommodation to DayAccommodationDTO
     */
    private DayAccommodationDTO convertDayAccommodationToDTO(SafariDayAccommodation entity) {
        DayAccommodationDTO dto = new DayAccommodationDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setAccommodationId(idObfuscator.encodeId(entity.getAccommodation().getId()));
        dto.setAccommodationName(entity.getAccommodation().getName());
        dto.setAccommodationSlug(entity.getAccommodation().getSlug());
        dto.setAccommodationRegion(entity.getAccommodation().getRegion());
        dto.setAccommodationDistrict(entity.getAccommodation().getDistrict());
        dto.setRoomTypeId(entity.getRoomType() != null ? idObfuscator.encodeId(entity.getRoomType().getId()) : null);
        dto.setRoomTypeName(entity.getRoomType() != null ? entity.getRoomType().getName() : null);
        dto.setRoomTypeMaxOccupancy(entity.getRoomType() != null ? entity.getRoomType().getMaxOccupancy() : null);
        dto.setRoomTypeMinOccupancy(entity.getRoomType() != null ? entity.getRoomType().getMinOccupancy() : null);
        dto.setRoomStandardId(entity.getRoomStandard() != null ? idObfuscator.encodeId(entity.getRoomStandard().getId()) : null);
        dto.setRoomStandardName(entity.getRoomStandard() != null ? entity.getRoomStandard().getName() : null);
        dto.setBoardTypeId(entity.getBoardType() != null ? idObfuscator.encodeId(entity.getBoardType().getId()) : null);
        dto.setBoardTypeName(entity.getBoardType() != null ? entity.getBoardType().getName() : null);
        dto.setRoomCount(entity.getRoomCount());
        dto.setIsAlternative(entity.getIsAlternative());
        dto.setNotes(entity.getNotes());

        // Safari-specific fields
        dto.setConfirmationNumber(entity.getConfirmationNumber());
        dto.setCheckInTime(entity.getConfirmedAt());
        dto.setCheckOutTime(null); // Not tracked separately at entity level
        dto.setActualRoomNumbers(entity.getRoomNumbers());
        dto.setGuestFeedback(entity.getGuestFeedback());

        return dto;
    }

    /**
     * Convert SafariDayParkActivity to ParkActivityDTO
     */
    private ParkActivityDTO convertParkActivityToDTO(SafariDayParkActivity entity) {
        ParkActivityDTO dto = new ParkActivityDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setActivityId(idObfuscator.encodeId(entity.getParkActivity().getActivity().getId()));
        dto.setActivityName(entity.getParkActivity().getActivity().getName());
        dto.setSortOrder(entity.getSortOrder());
        dto.setDurationHours(entity.getDurationHours());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setNotes(entity.getNotes());
        dto.setIsIncludedInPrice(entity.getIsIncludedInPrice());

        // Safari-specific fields
        dto.setWasCompleted(entity.getIsCompleted());
        dto.setActualStartTime(entity.getCompletedAt() != null ? entity.getCompletedAt().toString() : null);
        dto.setActualEndTime(null); // Not tracked at entity level
        dto.setCompletionNotes(entity.getSightingsNotes());

        return dto;
    }

    /**
     * Convert SafariDayParkTariff to ParkTariffDTO
     */
    private ParkTariffDTO convertParkTariffToDTO(SafariDayParkTariff entity) {
        ParkTariffDTO dto = new ParkTariffDTO();
        dto.setId(idObfuscator.encodeId(entity.getId()));
        dto.setParkId(idObfuscator.encodeId(entity.getParkTariff().getPark().getId()));
        dto.setParkName(entity.getParkTariff().getPark().getName());
        dto.setTariffId(idObfuscator.encodeId(entity.getParkTariff().getTariff().getId()));
        dto.setTariffName(entity.getParkTariff().getTariff().getName());
        dto.setNotes(entity.getNotes());
        dto.setIsIncludedInPrice(entity.getIsIncludedInPrice());

        // Safari-specific fields
        dto.setIsPaid(entity.getIsPaid());
        dto.setPaidAt(entity.getPaidAt());
        dto.setReceiptNumber(entity.getReceiptNumber());
        dto.setPaymentNotes(entity.getPaymentNotes());
        dto.setPaxCount(entity.getPaxCount());
        dto.setIsWaived(entity.getIsWaived());
        dto.setWaiverReason(entity.getWaiverReason());

        return dto;
    }

    /**
     * Get user display name from firstName and lastName
     */
    private String getUserDisplayName(com.itineraryledger.kabengosafaris.User.User user) {
        String name = "";
        if (user.getFirstName() != null) {
            name += user.getFirstName();
        }
        if (user.getLastName() != null) {
            if (!name.isEmpty()) {
                name += " ";
            }
            name += user.getLastName();
        }
        return !name.isEmpty() ? name : user.getUsername();
    }

    /**
     * Build CustomerDTO from Customer entity
     */
    private FullSafariDTO.CustomerDTO buildCustomerDTO(com.itineraryledger.kabengosafaris.Customer.Entity.Customer customer) {
        if (customer == null) {
            return null;
        }

        FullSafariDTO.CustomerDTO dto = new FullSafariDTO.CustomerDTO();
        dto.setId(idObfuscator.encodeId(customer.getId()));
        dto.setCode(customer.getCode());
        dto.setCustomerType(customer.getCustomerType());
        dto.setCustomerTypeDisplayName(customer.getCustomerType() != null ? customer.getCustomerType().getDisplayName() : null);
        dto.setTitle(customer.getTitle());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setCompanyName(customer.getCompanyName());
        dto.setDisplayName(customer.getDisplayName());
        dto.setPrimaryEmail(customer.getPrimaryEmail());
        dto.setPrimaryPhone(customer.getPrimaryPhone());
        dto.setNationality(customer.getNationality());
        dto.setResidency(customer.getResidency());
        dto.setPassportNumber(customer.getPassportNumber());
        dto.setPassportExpiry(customer.getPassportExpiry());
        dto.setDateOfBirth(customer.getDateOfBirth());

        // Compute passportExpiringSoon: true if passport expires within 6 months
        dto.setPassportExpiringSoon(computePassportExpiringSoon(customer.getPassportExpiry()));

        dto.setAddress(customer.getAddress());
        dto.setCity(customer.getCity());
        dto.setState(customer.getState());
        dto.setCountry(customer.getCountry());
        dto.setPostalCode(customer.getPostalCode());

        // Compute full address from individual fields
        dto.setFullAddress(computeFullAddress(customer));

        dto.setPreferredLanguage(customer.getPreferredLanguage());
        dto.setPreferredCurrency(customer.getPreferredCurrency());
        dto.setSource(customer.getSource());
        dto.setSourceDisplayName(customer.getSource() != null ? customer.getSource().getDisplayName() : null);
        dto.setReferredBy(customer.getReferredBy());
        dto.setDietaryRequirements(customer.getDietaryRequirements());
        dto.setMedicalConditions(customer.getMedicalConditions());
        dto.setSpecialRequests(customer.getSpecialRequests());
        dto.setInterests(customer.getInterests());
        dto.setIsVip(customer.getIsVip());

        return dto;
    }

    /**
     * Compute if passport is expiring soon (within 6 months)
     */
    private Boolean computePassportExpiringSoon(java.time.LocalDate passportExpiry) {
        if (passportExpiry == null) {
            return null;
        }
        java.time.LocalDate sixMonthsFromNow = java.time.LocalDate.now().plusMonths(6);
        return passportExpiry.isBefore(sixMonthsFromNow);
    }

    /**
     * Compute full address from address components
     */
    private String computeFullAddress(com.itineraryledger.kabengosafaris.Customer.Entity.Customer customer) {
        StringBuilder fullAddress = new StringBuilder();

        if (customer.getAddress() != null && !customer.getAddress().isBlank()) {
            fullAddress.append(customer.getAddress());
        }

        if (customer.getCity() != null && !customer.getCity().isBlank()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(customer.getCity());
        }

        if (customer.getState() != null && !customer.getState().isBlank()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(customer.getState());
        }

        if (customer.getPostalCode() != null && !customer.getPostalCode().isBlank()) {
            if (fullAddress.length() > 0) fullAddress.append(" ");
            fullAddress.append(customer.getPostalCode());
        }

        if (customer.getCountry() != null && !customer.getCountry().isBlank()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(customer.getCountry());
        }

        return fullAddress.length() > 0 ? fullAddress.toString() : null;
    }

    /**
     * Build ItineraryDTO from Itinerary entity
     */
    private FullSafariDTO.ItineraryDTO buildItineraryDTO(com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary itinerary) {
        if (itinerary == null) {
            return null;
        }

        FullSafariDTO.ItineraryDTO dto = new FullSafariDTO.ItineraryDTO();
        dto.setId(idObfuscator.encodeId(itinerary.getId()));
        dto.setName(itinerary.getName());
        dto.setCode(itinerary.getCode());
        dto.setStatus(itinerary.getStatus());
        dto.setStatusDisplayName(itinerary.getStatus() != null ? itinerary.getStatus().getDisplayName() : null);
        dto.setTripType(itinerary.getTripType());
        dto.setTripTypeDisplayName(itinerary.getTripType() != null ? itinerary.getTripType().getDisplayName() : null);
        dto.setTripTypeDescription(itinerary.getTripType() != null ? itinerary.getTripType().getDescription() : null);
        dto.setBudgetCategory(itinerary.getBudgetCategory());
        dto.setBudgetCategoryDisplayName(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDisplayName() : null);
        dto.setBudgetCategoryDescription(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getDescription() : null);
        dto.setBudgetCategoryTier(itinerary.getBudgetCategory() != null ? itinerary.getBudgetCategory().getTier() : null);
        dto.setTotalDays(itinerary.getTotalDays());
        dto.setTotalNights(itinerary.getTotalNights());
        dto.setIsDayTrip(itinerary.getTotalDays() != null && itinerary.getTotalNights() != null
            && itinerary.getTotalDays() == 1 && itinerary.getTotalNights() == 0);
        dto.setCarCount(itinerary.getCarCount());
        dto.setDescription(itinerary.getDescription());
        dto.setHighlights(itinerary.getHighlights());
        dto.setStartLocation(itinerary.getStartLocation());
        dto.setEndLocation(itinerary.getEndLocation());
        dto.setIsActive(itinerary.getIsActive());
        dto.setTotalPaxCount(itinerary.getTotalPaxCount());

        // Compute totalDaysCount from days list size, or null if days not loaded
        dto.setTotalDaysCount(itinerary.getDays() != null ? itinerary.getDays().size() : null);

        dto.setCreatedAt(itinerary.getCreatedAt());
        dto.setUpdatedAt(itinerary.getUpdatedAt());

        return dto;
    }
}
