package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteDayParkRepository
        extends JpaRepository<QuoteDayPark, Long>, JpaSpecificationExecutor<QuoteDayPark> {

    List<QuoteDayPark> findByQuoteDayIdOrderBySortOrderAsc(Long quoteDayId);

    long countByQuoteDayId(Long quoteDayId);
}
