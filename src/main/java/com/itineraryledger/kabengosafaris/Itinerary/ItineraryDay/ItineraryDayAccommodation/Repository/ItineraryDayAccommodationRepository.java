package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayAccommodationRepository extends JpaRepository<ItineraryDayAccommodation, Long>, JpaSpecificationExecutor<ItineraryDayAccommodation> {

    List<ItineraryDayAccommodation> findByItineraryDayId(Long itineraryDayId);

    List<ItineraryDayAccommodation> findByItineraryDayIdAndIsAlternativeFalse(Long itineraryDayId);

    List<ItineraryDayAccommodation> findByItineraryDayIdAndIsAlternativeTrue(Long itineraryDayId);

    void deleteByItineraryDayId(Long itineraryDayId);

    long countByItineraryDayId(Long itineraryDayId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within itinerary day)
    // ========================

    @Query("SELECT a.id FROM ItineraryDayAccommodation a WHERE a.itineraryDay.id = :parentId AND a.id > :currentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM ItineraryDayAccommodation a WHERE a.itineraryDay.id = :parentId AND a.id < :currentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM ItineraryDayAccommodation a WHERE a.itineraryDay.id = :parentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT a.id FROM ItineraryDayAccommodation a WHERE a.itineraryDay.id = :parentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);

    /**
     * Per-accommodation usage: distinct ACTIVE itineraries that stay at each accommodation.
     * Returns rows of [accommodationId (Long), count (Long)]. Powers the public "guest favourites".
     */
    @Query("SELECT da.accommodation.id, COUNT(DISTINCT da.itineraryDay.itinerary.id) " +
           "FROM ItineraryDayAccommodation da " +
           "WHERE da.accommodation.id IN :accIds AND da.itineraryDay.itinerary.isActive = true " +
           "GROUP BY da.accommodation.id")
    List<Object[]> countActiveItinerariesByAccommodationIds(@Param("accIds") List<Long> accIds);
}
