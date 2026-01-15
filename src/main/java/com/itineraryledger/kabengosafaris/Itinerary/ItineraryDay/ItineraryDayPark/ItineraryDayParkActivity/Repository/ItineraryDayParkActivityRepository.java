package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayParkActivityRepository extends JpaRepository<ItineraryDayParkActivity, Long>, JpaSpecificationExecutor<ItineraryDayParkActivity> {

    List<ItineraryDayParkActivity> findByItineraryDayParkIdOrderBySortOrderAsc(Long itineraryDayParkId);

    void deleteByItineraryDayParkId(Long itineraryDayParkId);

    long countByItineraryDayParkId(Long itineraryDayParkId);
}
