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

    /**
     * Distinct ACTIVE itinerary ids that stay at a given accommodation, whether it is the lodge the
     * trip is priced on or one offered as an alternative. Powers the public "safaris that stay
     * here" carousel.
     *
     * <p>Alternatives used to be excluded, on the reasoning that a backup is not really where the
     * trip stays. Measured against the live catalogue that reasoning cost far more than it saved:
     * 43 of 64 lodge pages showed an empty carousel, and a lodge like Kahawa House — offered on
     * day one of a published fourteen-day trip — appeared on no trip at all. A traveller who
     * searched for it found a page that ranked, described the property, and then had nothing to
     * sell them.
     *
     * <p>An alternative is a lodge we would genuinely put somebody in on that trip, at a different
     * price. That is a real answer to "which safaris stay here", so it belongs in the carousel;
     * the itinerary page is where the distinction between the two is drawn.
     */
    @Query("SELECT DISTINCT da.itineraryDay.itinerary.id " +
           "FROM ItineraryDayAccommodation da " +
           "WHERE da.accommodation.id = :accommodationId " +
           "AND da.itineraryDay.itinerary.isActive = true")
    List<Long> findActiveItineraryIdsByAccommodationId(@Param("accommodationId") Long accommodationId);
}
