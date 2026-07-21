package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayActivityRepository extends JpaRepository<ItineraryDayActivity, Long>, JpaSpecificationExecutor<ItineraryDayActivity> {

    List<ItineraryDayActivity> findByItineraryDayIdOrderBySortOrderAsc(Long itineraryDayId);

    boolean existsByItineraryDayIdAndActivityId(Long itineraryDayId, Long activityId);

    void deleteByItineraryDayId(Long itineraryDayId);

    long countByItineraryDayId(Long itineraryDayId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within itinerary day)
    // ========================

    @Query("SELECT a.id FROM ItineraryDayActivity a WHERE a.itineraryDay.id = :parentId AND a.id > :currentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM ItineraryDayActivity a WHERE a.itineraryDay.id = :parentId AND a.id < :currentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM ItineraryDayActivity a WHERE a.itineraryDay.id = :parentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT a.id FROM ItineraryDayActivity a WHERE a.itineraryDay.id = :parentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);

    /**
     * Per-activity usage: distinct ACTIVE itineraries that include each activity.
     * Returns rows of [activityId (Long), count (Long)]. Powers the public "most popular experiences".
     */
    @Query("SELECT da.activity.id, COUNT(DISTINCT da.itineraryDay.itinerary.id) " +
           "FROM ItineraryDayActivity da " +
           "WHERE da.activity.id IN :activityIds AND da.itineraryDay.itinerary.isActive = true " +
           "GROUP BY da.activity.id")
    List<Object[]> countActiveItinerariesByActivityIds(@Param("activityIds") List<Long> activityIds);

    /**
     * Distinct ACTIVE itinerary ids that feature a given activity.
     * Powers the public "safaris featuring this experience" section.
     */
    @Query("SELECT DISTINCT da.itineraryDay.itinerary.id FROM ItineraryDayActivity da " +
           "WHERE da.activity.id = :activityId AND da.itineraryDay.itinerary.isActive = true")
    List<Long> findActiveItineraryIdsByActivityId(@Param("activityId") Long activityId);
}
