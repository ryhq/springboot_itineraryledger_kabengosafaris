package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Services;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Repository.QuoteDayFlightRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Entity.QuoteDayFlight;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayTreeDTO.DayFlightDTO;
import com.itineraryledger.kabengosafaris.Flight.Repository.FlightFareRepository;
import com.itineraryledger.kabengosafaris.Flight.CostEstimation.FlightFarePricer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayTreeDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayTreeDTO.DayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayTreeDTO.DayActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayTreeDTO.DayParkDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayTreeDTO.ParkActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayTreeDTO.ParkTariffDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository.QuoteDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Repository.QuoteDayActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Repository.QuoteDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Repository.QuoteDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Repository.QuoteDayParkRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Repository.QuoteDayRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Every day of a quote, with everything on it, in one request.
 *
 * The day-configuration screen shows what each day is missing — a night with no
 * stay, a park with no fees — and that cannot be answered day by day without one
 * request per day plus one per park. A fourteen-day safari would be fifty
 * requests to draw one screen.
 *
 * <p>This is the quote's counterpart to {@code GET /itineraries/{id}/full}, and
 * returns the same shape, because it feeds the same screen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuoteDayTreeService {

    private final QuoteRepository quoteRepository;
    private final QuoteDayRepository quoteDayRepository;
    private final QuoteDayParkRepository quoteDayParkRepository;
    private final QuoteDayParkActivityRepository parkActivityRepository;
    private final QuoteDayParkTariffRepository parkTariffRepository;
    private final QuoteDayAccommodationRepository accommodationRepository;
    private final QuoteDayActivityRepository dayActivityRepository;
    private final QuoteDayFlightRepository flightRepository;
    private final FlightFareRepository flightFareRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getDayTree(String quoteIdObfuscated) {
        Long quoteId;
        try {
            quoteId = idObfuscator.decodeId(quoteIdObfuscated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID")
            );
        }

        if (!quoteRepository.existsById(quoteId)) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND")
            );
        }

        List<QuoteDayTreeDTO> days = quoteDayRepository
            .findByQuoteIdOrderByDayNumberAsc(quoteId)
            .stream()
            .map(this::buildDay)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("id", quoteIdObfuscated);
        response.put("days", days);
        response.put("totalDaysCount", days.size());

        return ResponseEntity.ok(
            ApiResponse.success(200, "Quote days retrieved successfully", response)
        );
    }

    private QuoteDayTreeDTO buildDay(QuoteDay day) {
        QuoteDayTreeDTO dto = QuoteDayTreeDTO.builder()
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
            .internalNotes(day.getInternalNotes())
            .createdAt(day.getCreatedAt())
            .build();

        List<QuoteDayActivity> activities = dayActivityRepository.findByQuoteDayIdOrderBySortOrderAsc(day.getId());
        if (!activities.isEmpty()) {
            dto.setActivities(activities.stream().map(this::toActivity).collect(Collectors.toList()));
        }

        List<QuoteDayAccommodation> stays = accommodationRepository.findByQuoteDayIdOrderByIdAsc(day.getId());
        if (!stays.isEmpty()) {
            dto.setAccommodations(stays.stream().map(this::toStay).collect(Collectors.toList()));
        }

        List<QuoteDayFlight> flights = flightRepository.findByQuoteDayIdOrderBySortOrderAscIdAsc(day.getId());
        if (!flights.isEmpty()) {
            dto.setFlights(flights.stream().map(this::toFlight).collect(Collectors.toList()));
        }

        List<QuoteDayPark> parks = quoteDayParkRepository.findByQuoteDayIdOrderBySortOrderAsc(day.getId());
        if (!parks.isEmpty()) {
            dto.setParks(parks.stream().map(this::toPark).collect(Collectors.toList()));
        }

        return dto;
    }

    private DayParkDTO toPark(QuoteDayPark visit) {
        DayParkDTO dto = DayParkDTO.builder()
            .id(idObfuscator.encodeId(visit.getId()))
            .parkId(idObfuscator.encodeId(visit.getPark().getId()))
            .parkName(visit.getPark().getName())
            .parkSlug(visit.getPark().getSlug())
            .entryType(visit.getEntryType())
            .entryTypeDisplayName(visit.getEntryType() != null ? visit.getEntryType().getDisplayName() : null)
            .sortOrder(visit.getSortOrder())
            .arrivalTime(visit.getArrivalTime())
            .departureTime(visit.getDepartureTime())
            .notes(visit.getNotes())
            .build();

        List<QuoteDayParkActivity> activities =
            parkActivityRepository.findByQuoteDayParkIdOrderBySortOrderAsc(visit.getId());
        if (!activities.isEmpty()) {
            dto.setActivities(activities.stream().map(this::toParkActivity).collect(Collectors.toList()));
        }

        List<QuoteDayParkTariff> tariffs = parkTariffRepository.findByQuoteDayParkIdOrderByIdAsc(visit.getId());
        if (!tariffs.isEmpty()) {
            dto.setTariffs(tariffs.stream().map(this::toParkTariff).collect(Collectors.toList()));
        }

        return dto;
    }

    private DayActivityDTO toActivity(QuoteDayActivity activity) {
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
     * The markup cascade is resolved here, as it is everywhere else a flight is read — line, then
     * fare, then airline — so the panel and the cost sheet cannot name different sources for the
     * same figure.
     */
    private DayFlightDTO toFlight(QuoteDayFlight flight) {
        var route = flight.getFlightRoute();
        var fare = flight.getFlightFare();
        if (fare == null && route != null) {
            /* No departure picked yet: the cheapest live one is what this day would cost today. */
            fare = flightFareRepository.findLiveForRoute(route.getId()).stream().findFirst().orElse(null);
        }
        var priced = FlightFarePricer.price(fare, route == null ? null : route.getAirline(),
            1, 0, flight.getMarkupType(), flight.getMarkupValue(), null);

        return DayFlightDTO.builder()
            .id(idObfuscator.encodeId(flight.getId()))
            .flightRouteId(route == null ? null : idObfuscator.encodeId(route.getId()))
            .flightFareId(flight.getFlightFare() == null
                ? null : idObfuscator.encodeId(flight.getFlightFare().getId()))
            .airlineName(route == null || route.getAirline() == null ? null : route.getAirline().getName())
            .sectorLabel(route == null ? null : route.getSectorLabel())
            .etd(fare == null || fare.getEtd() == null ? null : fare.getEtd().toString())
            .eta(fare == null || fare.getEta() == null ? null : fare.getEta().toString())
            .departureLabel(fare == null ? null : fare.getDepartureLabel())
            .displayName(flight.getDisplayName())
            .passengerCount(flight.getPassengerCount())
            .isAlternative(flight.getIsAlternative())
            .isIncludedInPrice(flight.getIsIncludedInPrice())
            .markupType(flight.getMarkupType() == null ? null : flight.getMarkupType().name())
            .markupValue(flight.getMarkupValue())
            .markupSource(priced.markup().source())
            .sortOrder(flight.getSortOrder())
            .notes(flight.getNotes())
            .sellingPerAdult(priced.sellingPerAdult())
            .build();
    }

    private DayAccommodationDTO toStay(QuoteDayAccommodation stay) {
        return DayAccommodationDTO.builder()
            .id(idObfuscator.encodeId(stay.getId()))
            .accommodationId(idObfuscator.encodeId(stay.getAccommodation().getId()))
            .accommodationName(stay.getAccommodation().getName())
            .accommodationSlug(stay.getAccommodation().getSlug())
            .roomTypeId(idObfuscator.encodeId(stay.getRoomType().getId()))
            .roomTypeName(stay.getRoomType().getName())
            .roomTypeMaxOccupancy(stay.getRoomType().getMaxOccupancy())
            .roomTypeMinOccupancy(stay.getRoomType().getMinOccupancy())
            .roomStandardId(idObfuscator.encodeId(stay.getRoomStandard().getId()))
            .roomStandardName(stay.getRoomStandard().getName())
            .boardTypeId(idObfuscator.encodeId(stay.getBoardType().getId()))
            .boardTypeName(stay.getBoardType().getName())
            .roomCount(stay.getRoomCount())
            .isAlternative(stay.getIsAlternative())
            .notes(stay.getNotes())
            .build();
    }

    private ParkActivityDTO toParkActivity(QuoteDayParkActivity activity) {
        return ParkActivityDTO.builder()
            .id(idObfuscator.encodeId(activity.getId()))
            .activityId(idObfuscator.encodeId(activity.getParkActivity().getActivity().getId()))
            .activityName(activity.getParkActivity().getActivity().getName())
            .sortOrder(activity.getSortOrder())
            .durationHours(activity.getDurationHours())
            .startTime(activity.getStartTime())
            .endTime(activity.getEndTime())
            .notes(activity.getNotes())
            .isIncludedInPrice(activity.getIsIncludedInPrice())
            .build();
    }

    private ParkTariffDTO toParkTariff(QuoteDayParkTariff tariff) {
        return ParkTariffDTO.builder()
            .id(idObfuscator.encodeId(tariff.getId()))
            .tariffId(idObfuscator.encodeId(tariff.getParkTariff().getTariff().getId()))
            .tariffName(tariff.getParkTariff().getTariff().getName())
            .notes(tariff.getNotes())
            .isIncludedInPrice(tariff.getIsIncludedInPrice())
            .build();
    }

    /** Kept so a caller with no days still gets a list rather than a null. */
    @SuppressWarnings("unused")
    private List<QuoteDayTreeDTO> empty() {
        return new ArrayList<>();
    }
}
