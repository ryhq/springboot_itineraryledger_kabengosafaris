package com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within safari day)
    // ========================

    @Query("SELECT a.id FROM SafariDayAccommodation a WHERE a.safariDay.id = :parentId AND a.id > :currentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM SafariDayAccommodation a WHERE a.safariDay.id = :parentId AND a.id < :currentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT a.id FROM SafariDayAccommodation a WHERE a.safariDay.id = :parentId ORDER BY a.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT a.id FROM SafariDayAccommodation a WHERE a.safariDay.id = :parentId ORDER BY a.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
