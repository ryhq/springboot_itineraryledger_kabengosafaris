package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long>, JpaSpecificationExecutor<ItineraryDay> {

    List<ItineraryDay> findByItineraryIdOrderByDayNumberAsc(Long itineraryId);

    Optional<ItineraryDay> findByItineraryIdAndDayNumber(Long itineraryId, Integer dayNumber);

    boolean existsByItineraryIdAndDayNumber(Long itineraryId, Integer dayNumber);

    boolean existsByItineraryIdAndDayNumberAndIdNot(Long itineraryId, Integer dayNumber, Long id);

    void deleteByItineraryId(Long itineraryId);

    long countByItineraryId(Long itineraryId);

    @Query("SELECT MAX(d.dayNumber) FROM ItineraryDay d WHERE d.itinerary.id = :itineraryId")
    Optional<Integer> findMaxDayNumberByItineraryId(@Param("itineraryId") Long itineraryId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within itinerary)
    // ========================

    @Query("SELECT d.id FROM ItineraryDay d WHERE d.itinerary.id = :parentId AND d.id > :currentId ORDER BY d.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT d.id FROM ItineraryDay d WHERE d.itinerary.id = :parentId AND d.id < :currentId ORDER BY d.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT d.id FROM ItineraryDay d WHERE d.itinerary.id = :parentId ORDER BY d.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT d.id FROM ItineraryDay d WHERE d.itinerary.id = :parentId ORDER BY d.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
