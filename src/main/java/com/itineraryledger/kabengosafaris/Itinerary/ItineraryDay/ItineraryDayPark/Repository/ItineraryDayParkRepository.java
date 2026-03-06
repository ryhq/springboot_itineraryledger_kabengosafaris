package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayParkRepository extends JpaRepository<ItineraryDayPark, Long>, JpaSpecificationExecutor<ItineraryDayPark> {

    List<ItineraryDayPark> findByItineraryDayIdOrderBySortOrderAsc(Long itineraryDayId);

    boolean existsByItineraryDayIdAndParkId(Long itineraryDayId, Long parkId);

    void deleteByItineraryDayId(Long itineraryDayId);

    long countByItineraryDayId(Long itineraryDayId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within itinerary day)
    // ========================

    @Query("SELECT p.id FROM ItineraryDayPark p WHERE p.itineraryDay.id = :parentId AND p.id > :currentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM ItineraryDayPark p WHERE p.itineraryDay.id = :parentId AND p.id < :currentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM ItineraryDayPark p WHERE p.itineraryDay.id = :parentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT p.id FROM ItineraryDayPark p WHERE p.itineraryDay.id = :parentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
