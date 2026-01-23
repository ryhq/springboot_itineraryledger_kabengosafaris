package com.itineraryledger.kabengosafaris.Safari.SafariDay.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariDay.Entity.SafariDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SafariDayRepository extends JpaRepository<SafariDay, Long>, JpaSpecificationExecutor<SafariDay> {

    List<SafariDay> findBySafariIdOrderByDayNumberAsc(Long safariId);

    Optional<SafariDay> findBySafariIdAndDayNumber(Long safariId, Integer dayNumber);

    Optional<SafariDay> findBySafariIdAndActualDate(Long safariId, LocalDate actualDate);

    boolean existsBySafariIdAndDayNumber(Long safariId, Integer dayNumber);

    @Query("SELECT MAX(sd.dayNumber) FROM SafariDay sd WHERE sd.safari.id = :safariId")
    Integer findMaxDayNumberBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT sd FROM SafariDay sd WHERE sd.safari.id = :safariId AND sd.isModified = true")
    List<SafariDay> findModifiedDaysBySafariId(@Param("safariId") Long safariId);

    @Query("SELECT sd FROM SafariDay sd WHERE sd.actualDate = :date")
    List<SafariDay> findByActualDate(@Param("date") LocalDate date);

    void deleteBySafariId(Long safariId);
}
