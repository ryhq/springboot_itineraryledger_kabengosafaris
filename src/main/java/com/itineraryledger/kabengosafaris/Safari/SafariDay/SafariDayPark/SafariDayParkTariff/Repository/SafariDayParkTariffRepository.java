package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayPark.SafariDayParkTariff.Entity.SafariDayParkTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafariDayParkTariffRepository extends JpaRepository<SafariDayParkTariff, Long>, JpaSpecificationExecutor<SafariDayParkTariff> {

    List<SafariDayParkTariff> findBySafariDayParkId(Long safariDayParkId);

    @Query("SELECT sdpt FROM SafariDayParkTariff sdpt WHERE sdpt.safariDayPark.id = :parkVisitId AND sdpt.isPaid = false AND sdpt.isWaived = false")
    List<SafariDayParkTariff> findUnpaidTariffsByParkVisitId(@Param("parkVisitId") Long parkVisitId);

    @Query("SELECT sdpt FROM SafariDayParkTariff sdpt WHERE sdpt.safariDayPark.safariDay.safari.id = :safariId")
    List<SafariDayParkTariff> findAllBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT sdpt FROM SafariDayParkTariff sdpt WHERE sdpt.safariDayPark.safariDay.safari.id = :safariId AND sdpt.isPaid = false AND sdpt.isWaived = false")
    List<SafariDayParkTariff> findAllUnpaidBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT SUM(sdpt.actualAmount) FROM SafariDayParkTariff sdpt WHERE sdpt.safariDayPark.safariDay.safari.id = :safariId AND sdpt.isPaid = true")
    java.math.BigDecimal getTotalPaidAmountBySafariId(@Param("safariId") Long safariId);

    void deleteBySafariDayParkId(Long safariDayParkId);
}
