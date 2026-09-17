package com.itineraryledger.kabengosafaris.Flight.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Flight.Entity.FlightRoute;

@Repository
public interface FlightRouteRepository extends JpaRepository<FlightRoute, Long>, JpaSpecificationExecutor<FlightRoute> {

    Optional<FlightRoute> findByCode(String code);

    /** The uniqueness the table enforces: one airline flies one sector once. */
    Optional<FlightRoute> findByAirlineIdAndOriginAirstripIdAndDestinationAirstripId(
        Long airlineId, Long originAirstripId, Long destinationAirstripId);

    boolean existsByAirlineIdAndOriginAirstripIdAndDestinationAirstripId(
        Long airlineId, Long originAirstripId, Long destinationAirstripId);

    List<FlightRoute> findByAirlineIdOrderByIdAsc(Long airlineId);
    long countByAirlineId(Long airlineId);

    /** "Who flies out of here" and "who flies in", for the airstrip record and the delete check. */
    long countByOriginAirstripIdOrDestinationAirstripId(Long originId, Long destinationId);
}
