package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long>, JpaSpecificationExecutor<ItineraryDay> {

    List<ItineraryDay> findByItineraryIdOrderByDayNumberAsc(Long itineraryId);

    Optional<ItineraryDay> findByItineraryIdAndDayNumber(Long itineraryId, Integer dayNumber);

    boolean existsByItineraryIdAndDayNumber(Long itineraryId, Integer dayNumber);

    boolean existsByItineraryIdAndDayNumberAndIdNot(Long itineraryId, Integer dayNumber, Long id);

    void deleteByItineraryId(Long itineraryId);

    long countByItineraryId(Long itineraryId);

    @Query("SELECT MAX(d.dayNumber) FROM ItineraryDay d WHERE d.itinerary.id = :itineraryId")
    Optional<Integer> findMaxDayNumberByItineraryId(@Param("itineraryId") Long itineraryId);
}
