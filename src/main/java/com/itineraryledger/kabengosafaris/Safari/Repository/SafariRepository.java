package com.itineraryledger.kabengosafaris.Safari.Repository;

import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SafariRepository extends JpaRepository<Safari, Long>, JpaSpecificationExecutor<Safari> {

    Optional<Safari> findBySlug(String slug);

    Optional<Safari> findByCode(String code);

    boolean existsBySlug(String slug);

    boolean existsByCode(String code);

    List<Safari> findByItineraryId(Long itineraryId);

    List<Safari> findByState(SafariState state);

    List<Safari> findByStateIn(List<SafariState> states);

    List<Safari> findByIsActiveTrue();

    List<Safari> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    List<Safari> findByEndDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT s FROM Safari s WHERE s.startDate <= :date AND s.endDate >= :date")
    List<Safari> findOngoingOnDate(@Param("date") LocalDate date);

    @Query("SELECT s FROM Safari s WHERE s.startDate > :date AND s.state IN :states")
    List<Safari> findUpcomingByStates(@Param("date") LocalDate date, @Param("states") List<SafariState> states);

    @Query("SELECT s FROM Safari s WHERE s.startDate BETWEEN :startDate AND :endDate")
    List<Safari> findByStartDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Safari s WHERE s.state = :state")
    long countByState(@Param("state") SafariState state);

    @Query("SELECT s FROM Safari s WHERE s.itinerary.id = :itineraryId AND s.state NOT IN ('CANCELLED', 'COMPLETED')")
    List<Safari> findActiveSafarisByItinerary(@Param("itineraryId") Long itineraryId);

    /**
     * Count active safaris
     */
    long countByIsActiveTrue();

    /**
     * Count safaris starting within a date range
     */
    long countByStartDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Count safaris that are ongoing (started but not ended) on a specific date
     */
    long countByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate startDate, LocalDate endDate);

    /**
     * Find safaris that should auto-start: FULLY_PAID with startDate <= today
     */
    @Query("SELECT s FROM Safari s WHERE s.state = 'FULLY_PAID' AND s.startDate <= :today")
    List<Safari> findReadyToStart(@Param("today") LocalDate today);

    /**
     * Find safaris that should auto-complete: IN_PROGRESS with endDate < today
     */
    @Query("SELECT s FROM Safari s WHERE s.state = 'IN_PROGRESS' AND s.endDate < :today")
    List<Safari> findReadyToComplete(@Param("today") LocalDate today);

    /**
     * Find safaris that should auto-close: COMPLETED with stateChangedAt older than given date
     */
    @Query("SELECT s FROM Safari s WHERE s.state = 'COMPLETED' AND s.stateChangedAt < :cutoff")
    List<Safari> findReadyToClose(@Param("cutoff") java.time.LocalDateTime cutoff);

    /**
     * Find safaris with payment gap: NOT FULLY_PAID but startDate <= today (should have been paid)
     */
    @Query("SELECT s FROM Safari s WHERE s.startDate <= :today AND s.state IN :unpaidStates AND s.isActive = true")
    List<Safari> findPaymentGapSafaris(@Param("today") LocalDate today, @Param("unpaidStates") List<SafariState> unpaidStates);

    /**
     * Find upcoming safaris in a specific phase window for alerts
     */
    @Query("SELECT s FROM Safari s WHERE s.startDate BETWEEN :fromDate AND :toDate AND s.state IN :states AND s.isActive = true")
    List<Safari> findUpcomingInWindow(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, @Param("states") List<SafariState> states);

    /**
     * Dashboard aggregations.
     * Returns rows [parkId, parkName, bookingCount] ranking parks by number of
     * distinct safaris that include them (via itinerary days). Excluded states
     * are typically DRAFT / CANCELLED so we only count meaningful bookings.
     */
    @Query("SELECT idp.park.id, idp.park.name, COUNT(DISTINCT s.id) " +
           "FROM Safari s JOIN s.itinerary i JOIN i.days d JOIN d.parks idp " +
           "WHERE s.state NOT IN :excludedStates " +
           "GROUP BY idp.park.id, idp.park.name " +
           "ORDER BY COUNT(DISTINCT s.id) DESC")
    List<Object[]> findTopParksByBookings(@Param("excludedStates") List<SafariState> excludedStates,
                                           org.springframework.data.domain.Pageable pageable);

    @Query("SELECT ida.activity.id, ida.activity.name, COUNT(DISTINCT s.id) " +
           "FROM Safari s JOIN s.itinerary i JOIN i.days d JOIN d.activities ida " +
           "WHERE s.state NOT IN :excludedStates " +
           "GROUP BY ida.activity.id, ida.activity.name " +
           "ORDER BY COUNT(DISTINCT s.id) DESC")
    List<Object[]> findTopActivitiesByBookings(@Param("excludedStates") List<SafariState> excludedStates,
                                                org.springframework.data.domain.Pageable pageable);

    @Query("SELECT idac.accommodation.id, idac.accommodation.name, COUNT(DISTINCT s.id) " +
           "FROM Safari s JOIN s.itinerary i JOIN i.days d JOIN d.accommodations idac " +
           "WHERE s.state NOT IN :excludedStates " +
           "GROUP BY idac.accommodation.id, idac.accommodation.name " +
           "ORDER BY COUNT(DISTINCT s.id) DESC")
    List<Object[]> findTopAccommodationsByBookings(@Param("excludedStates") List<SafariState> excludedStates,
                                                    org.springframework.data.domain.Pageable pageable);

    /**
     * Count safaris grouped by state where start date falls in range.
     * Returns [state, count] rows.
     */
    @Query("SELECT s.state, COUNT(s) FROM Safari s " +
           "WHERE (:from IS NULL OR s.createdAt >= :from) " +
           "  AND (:to IS NULL OR s.createdAt <= :to) " +
           "GROUP BY s.state")
    List<Object[]> countByStateGrouped(@Param("from") java.time.LocalDateTime from,
                                        @Param("to") java.time.LocalDateTime to);

    long countByCreatedAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

    // Navigation queries for next/previous
    @Query("SELECT e.id FROM Safari e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Safari e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Safari e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Safari e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
