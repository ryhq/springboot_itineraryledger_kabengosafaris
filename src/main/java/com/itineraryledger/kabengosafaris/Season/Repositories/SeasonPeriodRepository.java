package com.itineraryledger.kabengosafaris.Season.Repositories;

import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * SeasonPeriodRepository - Data access layer for SeasonPeriod entity
 *
 * Uses Specification pattern for all queries - see SeasonPeriodSpecification
 */
@Repository
public interface SeasonPeriodRepository extends JpaRepository<SeasonPeriod, Long>, JpaSpecificationExecutor<SeasonPeriod> {
    // All queries handled through Specifications
}
