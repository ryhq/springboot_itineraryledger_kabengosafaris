package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkActivity.Entity.SafariDayParkActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafariDayParkActivityRepository extends JpaRepository<SafariDayParkActivity, Long>, JpaSpecificationExecutor<SafariDayParkActivity> {

    List<SafariDayParkActivity> findBySafariDayParkIdOrderBySortOrderAsc(Long safariDayParkId);

    @Query("SELECT sdpa FROM SafariDayParkActivity sdpa WHERE sdpa.safariDayPark.id = :parkVisitId AND sdpa.isCompleted = true")
    List<SafariDayParkActivity> findCompletedActivitiesByParkVisitId(@Param("parkVisitId") Long parkVisitId);

    @Query("SELECT sdpa FROM SafariDayParkActivity sdpa WHERE sdpa.safariDayPark.id = :parkVisitId AND sdpa.isSkipped = true")
    List<SafariDayParkActivity> findSkippedActivitiesByParkVisitId(@Param("parkVisitId") Long parkVisitId);

    @Query("SELECT sdpa FROM SafariDayParkActivity sdpa WHERE sdpa.safariDayPark.safariDay.safari.id = :safariId ORDER BY sdpa.safariDayPark.safariDay.dayNumber, sdpa.safariDayPark.sortOrder, sdpa.sortOrder")
    List<SafariDayParkActivity> findAllBySafariId(@Param("safariId") Long safariId);

    void deleteBySafariDayParkId(Long safariDayParkId);
}
