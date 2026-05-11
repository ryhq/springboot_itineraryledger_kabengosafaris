package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteDayAccommodationRepository
        extends JpaRepository<QuoteDayAccommodation, Long>, JpaSpecificationExecutor<QuoteDayAccommodation> {

    List<QuoteDayAccommodation> findByQuoteDayIdOrderByIdAsc(Long quoteDayId);

    long countByQuoteDayId(Long quoteDayId);
}
