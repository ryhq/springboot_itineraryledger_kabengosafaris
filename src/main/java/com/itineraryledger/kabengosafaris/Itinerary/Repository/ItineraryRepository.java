package com.itineraryledger.kabengosafaris.Itinerary.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long>, JpaSpecificationExecutor<Itinerary> {

    Optional<Itinerary> findByCode(String code);

    Optional<Itinerary> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    long countByStatus(ItineraryStatus status);

    long countByIsActiveTrue();

    // Navigation queries for next/previous
    @Query("SELECT e.id FROM Itinerary e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Itinerary e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Itinerary e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Itinerary e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();

    List<Itinerary> findByStatusAndIsActiveTrue(ItineraryStatus status);
}
