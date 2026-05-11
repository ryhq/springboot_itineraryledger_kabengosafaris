package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteDayParkTariffRepository
        extends JpaRepository<QuoteDayParkTariff, Long>, JpaSpecificationExecutor<QuoteDayParkTariff> {

    List<QuoteDayParkTariff> findByQuoteDayParkIdOrderByIdAsc(Long quoteDayParkId);

    long countByQuoteDayParkId(Long quoteDayParkId);
}
