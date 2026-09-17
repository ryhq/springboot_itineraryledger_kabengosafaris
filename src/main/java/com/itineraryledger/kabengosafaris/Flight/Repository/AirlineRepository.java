package com.itineraryledger.kabengosafaris.Flight.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Flight.Entity.Airline;

@Repository
public interface AirlineRepository extends JpaRepository<Airline, Long>, JpaSpecificationExecutor<Airline> {

    Optional<Airline> findBySlug(String slug);
    Optional<Airline> findByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    List<Airline> findByIsActiveTrueOrderByNameAsc();
}
