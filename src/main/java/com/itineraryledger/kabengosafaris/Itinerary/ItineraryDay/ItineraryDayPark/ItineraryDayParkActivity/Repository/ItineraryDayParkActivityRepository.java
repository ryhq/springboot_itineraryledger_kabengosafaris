package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayParkActivityRepository extends JpaRepository<ItineraryDayParkActivity, Long>, JpaSpecificationExecutor<ItineraryDayParkActivity> {

    List<ItineraryDayParkActivity> findByItineraryDayParkIdOrderBySortOrderAsc(Long itineraryDayParkId);

    void deleteByItineraryDayParkId(Long itineraryDayParkId);

    long countByItineraryDayParkId(Long itineraryDayParkId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within park visit)
    // ========================

    @Query("SELECT a.id FROM ItineraryDayParkActivity a WHERE a.itineraryDayPark.id = :parentId AND a.id > :currentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM ItineraryDayParkActivity a WHERE a.itineraryDayPark.id = :parentId AND a.id < :currentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM ItineraryDayParkActivity a WHERE a.itineraryDayPark.id = :parentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT a.id FROM ItineraryDayParkActivity a WHERE a.itineraryDayPark.id = :parentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
