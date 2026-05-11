package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteDayParkActivityRepository
        extends JpaRepository<QuoteDayParkActivity, Long>, JpaSpecificationExecutor<QuoteDayParkActivity> {

    List<QuoteDayParkActivity> findByQuoteDayParkIdOrderBySortOrderAsc(Long quoteDayParkId);

    long countByQuoteDayParkId(Long quoteDayParkId);
}
