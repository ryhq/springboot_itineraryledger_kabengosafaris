package com.itineraryledger.kabengosafaris.Flight.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Flight.Entity.Airstrip;

@Repository
public interface AirstripRepository extends JpaRepository<Airstrip, Long>, JpaSpecificationExecutor<Airstrip> {

    /* The airline's three-letter code is the natural key, and what a bundle matches on. */
    Optional<Airstrip> findByCodeIgnoreCase(String code);
    Optional<Airstrip> findBySlug(String slug);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsBySlug(String slug);
    List<Airstrip> findByIsActiveTrueOrderByNameAsc();
}
