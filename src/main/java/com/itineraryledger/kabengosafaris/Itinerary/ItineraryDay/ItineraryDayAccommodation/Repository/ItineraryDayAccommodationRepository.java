package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayAccommodationRepository extends JpaRepository<ItineraryDayAccommodation, Long>, JpaSpecificationExecutor<ItineraryDayAccommodation> {

    List<ItineraryDayAccommodation> findByItineraryDayId(Long itineraryDayId);

    List<ItineraryDayAccommodation> findByItineraryDayIdAndIsAlternativeFalse(Long itineraryDayId);

    List<ItineraryDayAccommodation> findByItineraryDayIdAndIsAlternativeTrue(Long itineraryDayId);

    void deleteByItineraryDayId(Long itineraryDayId);

    long countByItineraryDayId(Long itineraryDayId);
}
