package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafariDayAccommodationRepository extends JpaRepository<SafariDayAccommodation, Long>, JpaSpecificationExecutor<SafariDayAccommodation> {

    List<SafariDayAccommodation> findBySafariDayId(Long safariDayId);

    @Query("SELECT sda FROM SafariDayAccommodation sda WHERE sda.safariDay.id = :dayId AND sda.isAlternative = false")
    List<SafariDayAccommodation> findPrimaryAccommodationsByDayId(@Param("dayId") Long dayId);

    @Query("SELECT sda FROM SafariDayAccommodation sda WHERE sda.safariDay.id = :dayId AND sda.isAlternative = true")
    List<SafariDayAccommodation> findAlternativeAccommodationsByDayId(@Param("dayId") Long dayId);

    @Query("SELECT sda FROM SafariDayAccommodation sda WHERE sda.safariDay.safari.id = :safariId")
    List<SafariDayAccommodation> findAllBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT sda FROM SafariDayAccommodation sda WHERE sda.safariDay.safari.id = :safariId AND sda.bookingStatus = :status")
    List<SafariDayAccommodation> findBySafariIdAndBookingStatus(@Param("safariId") Long safariId, @Param("status") BookingStatus status);

    @Query("SELECT sda FROM SafariDayAccommodation sda WHERE sda.safariDay.safari.id = :safariId AND sda.confirmationNumber IS NULL AND sda.isAlternative = false")
    List<SafariDayAccommodation> findUnconfirmedPrimaryAccommodations(@Param("safariId") Long safariId);

    void deleteBySafariDayId(Long safariDayId);
}
