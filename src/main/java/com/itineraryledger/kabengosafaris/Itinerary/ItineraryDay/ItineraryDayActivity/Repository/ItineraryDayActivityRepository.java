package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayActivityRepository extends JpaRepository<ItineraryDayActivity, Long>, JpaSpecificationExecutor<ItineraryDayActivity> {

    List<ItineraryDayActivity> findByItineraryDayIdOrderBySortOrderAsc(Long itineraryDayId);

    boolean existsByItineraryDayIdAndActivityId(Long itineraryDayId, Long activityId);

    void deleteByItineraryDayId(Long itineraryDayId);

    long countByItineraryDayId(Long itineraryDayId);
}
