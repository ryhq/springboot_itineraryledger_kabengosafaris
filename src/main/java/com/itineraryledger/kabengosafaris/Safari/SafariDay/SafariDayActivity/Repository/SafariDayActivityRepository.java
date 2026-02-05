package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayActivity.Entity.SafariDayActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafariDayActivityRepository extends JpaRepository<SafariDayActivity, Long>, JpaSpecificationExecutor<SafariDayActivity> {

    List<SafariDayActivity> findBySafariDayIdOrderBySortOrderAsc(Long safariDayId);

    boolean existsBySafariDayIdAndActivityId(Long safariDayId, Long activityId);

    long countBySafariDayId(Long safariDayId);

    @Query("SELECT sda FROM SafariDayActivity sda WHERE sda.safariDay.id = :dayId AND sda.isCompleted = true")
    List<SafariDayActivity> findCompletedActivitiesByDayId(@Param("dayId") Long dayId);

    @Query("SELECT sda FROM SafariDayActivity sda WHERE sda.safariDay.id = :dayId AND sda.isSkipped = true")
    List<SafariDayActivity> findSkippedActivitiesByDayId(@Param("dayId") Long dayId);

    @Query("SELECT sda FROM SafariDayActivity sda WHERE sda.safariDay.safari.id = :safariId ORDER BY sda.safariDay.dayNumber, sda.sortOrder")
    List<SafariDayActivity> findAllBySafariId(@Param("safariId") Long safariId);

    void deleteBySafariDayId(Long safariDayId);
}
