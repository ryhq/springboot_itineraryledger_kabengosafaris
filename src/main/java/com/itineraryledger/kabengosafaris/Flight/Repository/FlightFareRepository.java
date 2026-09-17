package com.itineraryledger.kabengosafaris.Flight.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Flight.Entity.FlightFare;

@Repository
public interface FlightFareRepository extends JpaRepository<FlightFare, Long>, JpaSpecificationExecutor<FlightFare> {

    Optional<FlightFare> findByCode(String code);
    List<FlightFare> findByFlightRouteIdOrderByEtdAsc(Long flightRouteId);
    long countByFlightRouteId(Long flightRouteId);

    /**
     * Every live fare for a sector on a date.
     *
     * <p>Retired rows are excluded here rather than by the caller: a retired fare exists only so a
     * quote already sent can still show the price it was sent with, and it must never be picked for
     * something new. Ordered cheapest first so a line that names a route but no departure gets the
     * cheapest one, which is the honest default for an itinerary that has no dates yet.
     */
    @Query("""
        SELECT f FROM FlightFare f
        WHERE f.flightRoute.id = :routeId
          AND f.isActive = true
          AND f.retiredAt IS NULL
          AND f.validFrom <= :date AND f.validTo >= :date
        ORDER BY CASE WHEN f.netFare IS NULL THEN 1 ELSE 0 END ASC, f.netFare ASC, f.id ASC
        """)
    List<FlightFare> findLiveForRouteOn(@Param("routeId") Long routeId, @Param("date") LocalDate date);

    /**
     * The same, with no date — for an itinerary, which is a product and has none.
     *
     * <p>Ordered so a fare that actually has a net rate wins over one that only has the airline's
     * published gross, because only the first of those can be quoted.
     */
    @Query("""
        SELECT f FROM FlightFare f
        WHERE f.flightRoute.id = :routeId
          AND f.isActive = true
          AND f.retiredAt IS NULL
        ORDER BY CASE WHEN f.netFare IS NULL THEN 1 ELSE 0 END ASC, f.netFare ASC, f.id ASC
        """)
    List<FlightFare> findLiveForRoute(@Param("routeId") Long routeId);

    /** Rows a new price list supersedes. Retired, never deleted — a sent quote points at them. */
    @Query("""
        SELECT f FROM FlightFare f
        WHERE f.flightRoute.airline.id = :airlineId
          AND f.retiredAt IS NULL
          AND f.validTo < :before
        """)
    List<FlightFare> findSupersededBy(@Param("airlineId") Long airlineId, @Param("before") LocalDate before);
}
