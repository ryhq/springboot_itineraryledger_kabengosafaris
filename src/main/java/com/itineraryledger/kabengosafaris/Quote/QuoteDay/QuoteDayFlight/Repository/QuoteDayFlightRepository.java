package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayFlight.Entity.QuoteDayFlight;

@Repository
public interface QuoteDayFlightRepository
        extends JpaRepository<QuoteDayFlight, Long>, JpaSpecificationExecutor<QuoteDayFlight> {

    List<QuoteDayFlight> findByQuoteDayIdOrderBySortOrderAscIdAsc(Long quoteDayId);
    void deleteByQuoteDayId(Long quoteDayId);
    long countByQuoteDayId(Long quoteDayId);
    long countByFlightRouteId(Long flightRouteId);
}
