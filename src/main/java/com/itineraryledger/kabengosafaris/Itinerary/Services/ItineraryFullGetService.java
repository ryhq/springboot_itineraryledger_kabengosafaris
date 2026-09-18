package com.itineraryledger.kabengosafaris.Itinerary.Services;

import com.itineraryledger.kabengosafaris.Inclusion.Services.InclusionSnapshotService;

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

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Flight.CostEstimation.FlightFarePricer;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightFareRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Entity.ItineraryDayFlight;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Repository.ItineraryDayFlightRepository;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.FullItineraryDTO.*;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository.ItineraryDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository.ItineraryDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository.ItineraryPaxRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ItineraryFullGetService - Service for retrieving complete itinerary with all nested data
 *
 * Returns the full itinerary structure including:
 * - Itinerary base data
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
public class ItineraryFullGetService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryDayRepository dayRepository;
    private final ItineraryPaxRepository paxRepository;
    private final ItineraryDayActivityRepository dayActivityRepository;
    private final ItineraryDayAccommodationRepository dayAccommodationRepository;
    private final ItineraryDayParkRepository dayParkRepository;
    private final ItineraryDayParkActivityRepository parkActivityRepository;
    private final ItineraryDayParkTariffRepository parkTariffRepository;
    private final ItineraryDayFlightRepository dayFlightRepository;
    private final FlightFareRepository flightFareRepository;
    private final IdObfuscator idObfuscator;
    private final InclusionSnapshotService inclusionSnapshot;

    @Autowired
    public ItineraryFullGetService(
        ItineraryRepository itineraryRepository,
        ItineraryDayRepository dayRepository,
        ItineraryPaxRepository paxRepository,
        ItineraryDayActivityRepository dayActivityRepository,
        ItineraryDayAccommodationRepository dayAccommodationRepository,
        ItineraryDayParkRepository dayParkRepository,
        ItineraryDayParkActivityRepository parkActivityRepository,
        ItineraryDayParkTariffRepository parkTariffRepository,
        ItineraryDayFlightRepository dayFlightRepository,
        FlightFareRepository flightFareRepository,
        IdObfuscator idObfuscator,
        InclusionSnapshotService inclusionSnapshot
    ) {
        this.itineraryRepository = itineraryRepository;
        this.dayRepository = dayRepository;
        this.paxRepository = paxRepository;
        this.dayActivityRepository = dayActivityRepository;
        this.dayAccommodationRepository = dayAccommodationRepository;
        this.dayParkRepository = dayParkRepository;
        this.parkActivityRepository = parkActivityRepository;
        this.parkTariffRepository = parkTariffRepository;
        this.dayFlightRepository = dayFlightRepository;
        this.flightFareRepository = flightFareRepository;
        this.idObfuscator = idObfuscator;
        this.inclusionSnapshot = inclusionSnapshot;
    }

    /**
     * Get complete itinerary with all nested data by obfuscated ID
     *
     * @param idObfuscated The obfuscated itinerary ID
     * @return ResponseEntity with ApiResponse containing the full itinerary
     */
    public ResponseEntity<ApiResponse<?>> getFullItinerary(String idObfuscated) {
        log.info("Fetching full itinerary with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            // Find itinerary
            Itinerary itinerary = itineraryRepository.findById(id).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Build full DTO
            FullItineraryDTO fullDTO = buildFullItineraryDTO(itinerary);

            log.info("Full itinerary retrieved successfully: {} with {} days, {} pax configurations",
                itinerary.getName(),
                fullDTO.getTotalDaysCount(),
                fullDTO.getTotalPaxCount());

            // Build navigation
            Long nextId = itineraryRepository.findNextId(id).orElse(null);
            Long previousId = itineraryRepository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = itineraryRepository.findFirstId().orElse(null);
            if (previousId == null) previousId = itineraryRepository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("itinerary", fullDTO);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Full itinerary retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error fetching full itinerary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch full itinerary", "FULL_ITINERARY_FETCH_FAILED")
            );
        }
    }

    /**
     * Build the complete FullItineraryDTO with all nested data
     */
    private FullItineraryDTO buildFullItineraryDTO(Itinerary itinerary) {
        FullItineraryDTO dto = new FullItineraryDTO();

        // ========================
        // ITINERARY BASE FIELDS
        // ========================
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
        /*
         * What the price covers, snapshotted onto this document when it was produced. Never read
         * live from the catalogue: a customer holds a copy of this, and an edit that changed what
         * our copy says would make ours the one that looked altered.
         */
        dto.setInclusions(inclusionSnapshot.linesOf(itinerary));

        dto.setStartLocation(itinerary.getStartLocation());
        dto.setEndLocation(itinerary.getEndLocation());
        dto.setIsActive(itinerary.getIsActive());
        dto.setCreatedAt(itinerary.getCreatedAt());
        dto.setUpdatedAt(itinerary.getUpdatedAt());

        // ========================
        // PAX LIST
        // ========================
        List<ItineraryPax> paxList = paxRepository.findByItineraryId(itinerary.getId());
        List<PaxDTO> paxDTOs = paxList.stream()
            .map(this::convertPaxToDTO)
            .collect(Collectors.toList());
        dto.setPaxList(paxDTOs);

        // ========================
        // DAYS WITH NESTED DATA
        // ========================
        List<ItineraryDay> days = dayRepository.findByItineraryIdOrderByDayNumberAsc(itinerary.getId());
        List<DayDTO> dayDTOs = new ArrayList<>();

        int totalParksCount = 0;
        int totalActivitiesCount = 0;
        int totalAccommodationsCount = 0;

        for (ItineraryDay day : days) {
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
                // Count park activities
                for (DayParkDTO parkDTO : dayDTO.getParks()) {
                    if (parkDTO.getActivities() != null) {
                        totalActivitiesCount += parkDTO.getActivities().size();
                    }
                }
            }
        }
        dto.setDays(dayDTOs);

        // ========================
        // SUMMARY STATISTICS
        // ========================
        dto.setTotalPaxCount(paxList.stream()
            .mapToInt(pax -> pax.getCount() != null ? pax.getCount() : 0)
            .sum());
        dto.setTotalDaysCount(days.size());
        dto.setTotalParksCount(totalParksCount);
        dto.setTotalActivitiesCount(totalActivitiesCount);
        dto.setTotalAccommodationsCount(totalAccommodationsCount);

        return dto;
    }

    /**
     * Build DayDTO with all nested activities, accommodations, and parks
     */
    private DayDTO buildDayDTO(ItineraryDay day) {
        DayDTO dto = DayDTO.builder()
            .id(idObfuscator.encodeId(day.getId()))
            .dayNumber(day.getDayNumber())
            .dayTag(day.getDayTag())
            .title(day.getTitle())
            .description(day.getDescription())
            .morningActivities(day.getMorningActivities())
            .afternoonActivities(day.getAfternoonActivities())
            .eveningActivities(day.getEveningActivities())
            .wildlifeHighlights(day.getWildlifeHighlights())
            .scenicHighlights(day.getScenicHighlights())
            .specialNotes(day.getSpecialNotes())
            .startLocation(day.getStartLocation())
            .endLocation(day.getEndLocation())
            .distanceKm(day.getDistanceKm())
            .isOvernight(day.getIsOvernight())
            .mealsIncluded(day.getMealsIncluded())
            .createdAt(day.getCreatedAt())
            .build();

        // Day Activities (standalone, not in park)
        List<ItineraryDayActivity> dayActivities = dayActivityRepository.findByItineraryDayIdOrderBySortOrderAsc(day.getId());
        if (!dayActivities.isEmpty()) {
            dto.setActivities(dayActivities.stream()
                .map(this::convertDayActivityToDTO)
                .collect(Collectors.toList()));
        }

        // Day Accommodations
        List<ItineraryDayAccommodation> accommodations = dayAccommodationRepository.findByItineraryDayId(day.getId());
        if (!accommodations.isEmpty()) {
            dto.setAccommodations(accommodations.stream()
                .map(this::convertAccommodationToDTO)
                .collect(Collectors.toList()));
        }

        // Day Parks with nested activities and tariffs
        List<ItineraryDayPark> parks = dayParkRepository.findByItineraryDayIdOrderBySortOrderAsc(day.getId());
        if (!parks.isEmpty()) {
            dto.setParks(parks.stream()
                .map(this::buildDayParkDTO)
                .collect(Collectors.toList()));
        }

        // Day Flights
        List<ItineraryDayFlight> flights = dayFlightRepository.findByItineraryDayIdOrderBySortOrderAscIdAsc(day.getId());
        if (!flights.isEmpty()) {
            dto.setFlights(flights.stream()
                .map(this::convertDayFlightToDTO)
                .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * A flight on a day, with its fare and the markup actually in force.
     *
     * <p>The markup is resolved HERE, once, rather than left for the calculator: it is a cascade
     * from this line to the fare to the airline, and resolving it in two places is how the cost
     * sheet and the panel come to disagree about which one applied.
     *
     * <p>A line with a route but no chosen fare still prices. An itinerary is a product and has no
     * dates, so the planner picks the sector and the office picks the departure later; until then
     * the cheapest live fare for that sector is the honest figure, and the DTO carries it.
     */
    private FullItineraryDTO.DayFlightDTO convertDayFlightToDTO(ItineraryDayFlight flight) {
        var route = flight.getFlightRoute();
        var airline = route == null ? null : route.getAirline();

        var fare = flight.getFlightFare();
        if (fare == null && route != null) {
            fare = flightFareRepository.findLiveForRoute(route.getId()).stream().findFirst().orElse(null);
        }

        var markup = FlightFarePricer.resolveMarkup(
            flight.getMarkupType(), flight.getMarkupValue(), fare, airline);

        return FullItineraryDTO.DayFlightDTO.builder()
            .id(idObfuscator.encodeId(flight.getId()))
            .flightRouteId(route == null ? null : idObfuscator.encodeId(route.getId()))
            .flightFareId(fare == null ? null : idObfuscator.encodeId(fare.getId()))
            .airlineName(airline == null ? null : airline.getName())
            .sectorLabel(route == null ? null : route.getSectorLabel())
            .originCode(route == null || route.getOriginAirstrip() == null
                ? null : route.getOriginAirstrip().getCode())
            .destinationCode(route == null || route.getDestinationAirstrip() == null
                ? null : route.getDestinationAirstrip().getCode())
            .etd(fare == null || fare.getEtd() == null ? null : fare.getEtd().toString())
            .eta(fare == null || fare.getEta() == null ? null : fare.getEta().toString())
            .departureLabel(fare == null ? null : fare.getDepartureLabel())
            .netFare(fare == null ? null : fare.getNetFare())
            .taxesAndFees(fare == null ? null : fare.getTaxesAndFees())
            .childPercent(fare == null ? null : fare.getChildPercent())
            .currency(fare == null ? null : fare.getCurrency())
            .markupType(markup.isNone() ? null : markup.type().name())
            .markupValue(markup.value())
            .markupSource(markup.source())
            .passengerCount(flight.getPassengerCount())
            .sortOrder(flight.getSortOrder())
            .notes(flight.getNotes())
            .isAlternative(flight.getIsAlternative())
            .isIncludedInPrice(flight.getIsIncludedInPrice())
            .isOnRequest(route == null ? null : route.getIsOnRequest())
            .minimumSeats(fare != null && fare.getMinimumSeats() != null
                ? fare.getMinimumSeats() : (route == null ? null : route.getMinimumSeats()))
            .operatingMonths(fare == null ? null : fare.getOperatingMonths())
            .build();
    }

    /**
     * Build DayParkDTO with nested activities and tariffs
     */
    private DayParkDTO buildDayParkDTO(ItineraryDayPark dayPark) {
        DayParkDTO dto = DayParkDTO.builder()
            .id(idObfuscator.encodeId(dayPark.getId()))
            .parkId(idObfuscator.encodeId(dayPark.getPark().getId()))
            .parkName(dayPark.getPark().getName())
            .parkSlug(dayPark.getPark().getSlug())
            .entryType(dayPark.getEntryType())
            .entryTypeDisplayName(dayPark.getEntryType().getDisplayName())
            .sortOrder(dayPark.getSortOrder())
            .arrivalTime(dayPark.getArrivalTime())
            .departureTime(dayPark.getDepartureTime())
            .notes(dayPark.getNotes())
            .build();

        // Park Activities
        List<ItineraryDayParkActivity> parkActivities = parkActivityRepository.findByItineraryDayParkIdOrderBySortOrderAsc(dayPark.getId());
        if (!parkActivities.isEmpty()) {
            dto.setActivities(parkActivities.stream()
                .map(this::convertParkActivityToDTO)
                .collect(Collectors.toList()));
        }

        // Park Tariffs
        List<ItineraryDayParkTariff> parkTariffs = parkTariffRepository.findByItineraryDayParkId(dayPark.getId());
        if (!parkTariffs.isEmpty()) {
            dto.setTariffs(parkTariffs.stream()
                .map(this::convertParkTariffToDTO)
                .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Convert ItineraryPax to PaxDTO
     */
    private PaxDTO convertPaxToDTO(ItineraryPax pax) {
        return PaxDTO.builder()
            .id(idObfuscator.encodeId(pax.getId()))
            .nationCategoryId(idObfuscator.encodeId(pax.getNationCategory().getId()))
            .nationCategoryName(pax.getNationCategory().getName())
            .ageCategoryId(idObfuscator.encodeId(pax.getAgeCategory().getId()))
            .ageCategoryName(pax.getAgeCategory().getName())
            .count(pax.getCount())
            .notes(pax.getNotes())
            .build();
    }

    /**
     * Convert ItineraryDayActivity to DayActivityDTO
     */
    private DayActivityDTO convertDayActivityToDTO(ItineraryDayActivity activity) {
        return DayActivityDTO.builder()
            .id(idObfuscator.encodeId(activity.getId()))
            .activityId(idObfuscator.encodeId(activity.getActivity().getId()))
            .activityName(activity.getActivity().getName())
            .activitySlug(activity.getActivity().getSlug())
            .sortOrder(activity.getSortOrder())
            .durationHours(activity.getDurationHours())
            .startTime(activity.getStartTime())
            .endTime(activity.getEndTime())
            .notes(activity.getNotes())
            .isIncludedInPrice(activity.getIsIncludedInPrice())
            .isOptional(activity.getIsOptional())
            .build();
    }

    /**
     * Convert ItineraryDayAccommodation to DayAccommodationDTO
     */
    private DayAccommodationDTO convertAccommodationToDTO(ItineraryDayAccommodation accommodation) {
        return DayAccommodationDTO.builder()
            .id(idObfuscator.encodeId(accommodation.getId()))
            .accommodationId(idObfuscator.encodeId(accommodation.getAccommodation().getId()))
            .accommodationName(accommodation.getAccommodation().getName())
            .accommodationSlug(accommodation.getAccommodation().getSlug())
            .accommodationRegion(accommodation.getAccommodation().getRegion())
            .accommodationDistrict(accommodation.getAccommodation().getDistrict())
            .accommodationCategory(accommodation.getAccommodation().getCategory() == null ? null
                : accommodation.getAccommodation().getCategory().name())
            .roomTypeId(idObfuscator.encodeId(accommodation.getRoomType().getId()))
            .roomTypeName(accommodation.getRoomType().getName())
            .roomTypeMaxOccupancy(accommodation.getRoomType().getMaxOccupancy())
            .roomTypeMinOccupancy(accommodation.getRoomType().getMinOccupancy())
            .roomStandardId(idObfuscator.encodeId(accommodation.getRoomStandard().getId()))
            .roomStandardName(accommodation.getRoomStandard().getName())
            .boardTypeId(idObfuscator.encodeId(accommodation.getBoardType().getId()))
            .boardTypeName(accommodation.getBoardType().getName())
            .roomCount(accommodation.getRoomCount())
            .isAlternative(accommodation.getIsAlternative())
            .notes(accommodation.getNotes())
            .build();
    }

    /**
     * Convert ItineraryDayParkActivity to ParkActivityDTO
     */
    private ParkActivityDTO convertParkActivityToDTO(ItineraryDayParkActivity parkActivity) {
        return ParkActivityDTO.builder()
            .id(idObfuscator.encodeId(parkActivity.getId()))
            .activityId(idObfuscator.encodeId(parkActivity.getParkActivity().getActivity().getId()))
            .activityName(parkActivity.getParkActivity().getActivity().getName())
            .sortOrder(parkActivity.getSortOrder())
            .durationHours(parkActivity.getDurationHours())
            .startTime(parkActivity.getStartTime())
            .endTime(parkActivity.getEndTime())
            .notes(parkActivity.getNotes())
            .isIncludedInPrice(parkActivity.getIsIncludedInPrice())
            .build();
    }

    /**
     * Convert ItineraryDayParkTariff to ParkTariffDTO
     */
    private ParkTariffDTO convertParkTariffToDTO(ItineraryDayParkTariff parkTariff) {
        return ParkTariffDTO.builder()
            .id(idObfuscator.encodeId(parkTariff.getId()))
            .tariffId(idObfuscator.encodeId(parkTariff.getParkTariff().getTariff().getId()))
            .tariffName(parkTariff.getParkTariff().getTariff().getName())
            .notes(parkTariff.getNotes())
            .isIncludedInPrice(parkTariff.getIsIncludedInPrice())
            .build();
    }
}
