package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteDayActivityRepository
        extends JpaRepository<QuoteDayActivity, Long>, JpaSpecificationExecutor<QuoteDayActivity> {

    List<QuoteDayActivity> findByQuoteDayIdOrderBySortOrderAsc(Long quoteDayId);

    long countByQuoteDayId(Long quoteDayId);
}
