package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafariDayParkRepository extends JpaRepository<SafariDayPark, Long>, JpaSpecificationExecutor<SafariDayPark> {

    List<SafariDayPark> findBySafariDayIdOrderBySortOrderAsc(Long safariDayId);

    boolean existsBySafariDayIdAndParkId(Long safariDayId, Long parkId);

    long countBySafariDayId(Long safariDayId);

    @Query("SELECT sdp FROM SafariDayPark sdp WHERE sdp.safariDay.safari.id = :safariId ORDER BY sdp.safariDay.dayNumber, sdp.sortOrder")
    List<SafariDayPark> findAllBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT sdp FROM SafariDayPark sdp WHERE sdp.safariDay.safari.id = :safariId AND sdp.entryType = :entryType")
    List<SafariDayPark> findBySafariIdAndEntryType(@Param("safariId") Long safariId, @Param("entryType") ParkEntryType entryType);

    @Query("SELECT sdp FROM SafariDayPark sdp WHERE sdp.safariDay.safari.id = :safariId AND sdp.feesPaid = false")
    List<SafariDayPark> findUnpaidParkVisits(@Param("safariId") Long safariId);

    @Query("SELECT DISTINCT sdp.park.id FROM SafariDayPark sdp WHERE sdp.safariDay.safari.id = :safariId")
    List<Long> findDistinctParkIdsBySafariId(@Param("safariId") Long safariId);

    void deleteBySafariDayId(Long safariDayId);

    // ========================
    // NAVIGATION QUERIES (parent-scoped circular next/previous)
    // ========================

    @Query("SELECT p.id FROM SafariDayPark p WHERE p.safariDay.id = :parentId AND p.id > :currentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM SafariDayPark p WHERE p.safariDay.id = :parentId AND p.id < :currentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM SafariDayPark p WHERE p.safariDay.id = :parentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT p.id FROM SafariDayPark p WHERE p.safariDay.id = :parentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
