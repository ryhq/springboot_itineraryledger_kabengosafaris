package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayFlight.Entity.ItineraryDayFlight;

@Repository
public interface ItineraryDayFlightRepository
        extends JpaRepository<ItineraryDayFlight, Long>, JpaSpecificationExecutor<ItineraryDayFlight> {

    List<ItineraryDayFlight> findByItineraryDayIdOrderBySortOrderAscIdAsc(Long itineraryDayId);
    void deleteByItineraryDayId(Long itineraryDayId);
    long countByItineraryDayId(Long itineraryDayId);

    /** A route in use cannot be deleted — the same reference check every catalogue here does. */
    long countByFlightRouteId(Long flightRouteId);
    long countByFlightFareId(Long flightFareId);
}
