package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafariDayParkTariffRepository extends JpaRepository<SafariDayParkTariff, Long>, JpaSpecificationExecutor<SafariDayParkTariff> {

    List<SafariDayParkTariff> findBySafariDayParkId(Long safariDayParkId);

    @Query("SELECT sdpt FROM SafariDayParkTariff sdpt WHERE sdpt.safariDayPark.id = :parkVisitId AND sdpt.isPaid = false AND sdpt.isWaived = false")
    List<SafariDayParkTariff> findUnpaidTariffsByParkVisitId(@Param("parkVisitId") Long parkVisitId);

    @Query("SELECT sdpt FROM SafariDayParkTariff sdpt WHERE sdpt.safariDayPark.safariDay.safari.id = :safariId")
    List<SafariDayParkTariff> findAllBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT sdpt FROM SafariDayParkTariff sdpt WHERE sdpt.safariDayPark.safariDay.safari.id = :safariId AND sdpt.isPaid = false AND sdpt.isWaived = false")
    List<SafariDayParkTariff> findAllUnpaidBySafariId(@Param("safariId") Long safariId);

    void deleteBySafariDayParkId(Long safariDayParkId);

    // ========================
    // NAVIGATION QUERIES (parent-scoped circular next/previous)
    // ========================

    @Query("SELECT t.id FROM SafariDayParkTariff t WHERE t.safariDayPark.id = :parentId AND t.id > :currentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT t.id FROM SafariDayParkTariff t WHERE t.safariDayPark.id = :parentId AND t.id < :currentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT t.id FROM SafariDayParkTariff t WHERE t.safariDayPark.id = :parentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT t.id FROM SafariDayParkTariff t WHERE t.safariDayPark.id = :parentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
