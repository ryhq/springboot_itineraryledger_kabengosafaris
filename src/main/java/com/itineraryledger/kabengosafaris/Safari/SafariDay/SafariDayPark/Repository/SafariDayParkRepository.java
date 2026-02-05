package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.Entity.SafariDayPark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
