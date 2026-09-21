package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayFlight.Entity.SafariDayFlight;

@Repository
public interface SafariDayFlightRepository
        extends JpaRepository<SafariDayFlight, Long>, JpaSpecificationExecutor<SafariDayFlight> {

    List<SafariDayFlight> findBySafariDayIdOrderBySortOrderAscIdAsc(Long safariDayId);
    void deleteBySafariDayId(Long safariDayId);
    long countBySafariDayId(Long safariDayId);
    long countByFlightRouteId(Long flightRouteId);

    /** Seats still not ticketed — what an operations list is actually asking for. */
    List<SafariDayFlight> findByBookingStatusNot(SafariDayFlight.BookingStatus status);
}
