package com.itineraryledger.kabengosafaris.Season.Repositories;

import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SeasonPeriodRepository - Data access layer for SeasonPeriod entity
 *
 * Uses Specification pattern for all queries - see SeasonPeriodSpecification
 */
@Repository
public interface SeasonPeriodRepository extends JpaRepository<SeasonPeriod, Long>, JpaSpecificationExecutor<SeasonPeriod> {
    // All queries handled through Specifications

    @Query("SELECT e.id FROM SeasonPeriod e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM SeasonPeriod e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM SeasonPeriod e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM SeasonPeriod e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
