package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayParkTariffRepository extends JpaRepository<ItineraryDayParkTariff, Long>, JpaSpecificationExecutor<ItineraryDayParkTariff> {

    List<ItineraryDayParkTariff> findByItineraryDayParkId(Long itineraryDayParkId);

    void deleteByItineraryDayParkId(Long itineraryDayParkId);

    long countByItineraryDayParkId(Long itineraryDayParkId);
}
