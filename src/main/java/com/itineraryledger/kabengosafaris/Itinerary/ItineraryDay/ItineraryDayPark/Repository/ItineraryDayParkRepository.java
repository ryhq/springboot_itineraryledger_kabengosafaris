package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayParkRepository extends JpaRepository<ItineraryDayPark, Long>, JpaSpecificationExecutor<ItineraryDayPark> {

    List<ItineraryDayPark> findByItineraryDayIdOrderBySortOrderAsc(Long itineraryDayId);

    boolean existsByItineraryDayIdAndParkId(Long itineraryDayId, Long parkId);

    void deleteByItineraryDayId(Long itineraryDayId);

    long countByItineraryDayId(Long itineraryDayId);
}
