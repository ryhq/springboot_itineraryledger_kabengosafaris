package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Entity.ItineraryCostSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryCostSummaryRepository extends JpaRepository<ItineraryCostSummary, Long> {

    List<ItineraryCostSummary> findByItineraryId(Long itineraryId);

    List<ItineraryCostSummary> findByItinerary_IdIn(List<Long> itineraryIds);

    void deleteByItineraryId(Long itineraryId);
}
