package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteDayRepository extends JpaRepository<QuoteDay, Long>, JpaSpecificationExecutor<QuoteDay> {

    List<QuoteDay> findByQuoteIdOrderByDayNumberAsc(Long quoteId);

    long countByQuoteId(Long quoteId);

    Optional<QuoteDay> findByQuoteIdAndDayNumber(Long quoteId, Integer dayNumber);

    @Query("SELECT MAX(d.dayNumber) FROM QuoteDay d WHERE d.quote.id = :quoteId")
    Optional<Integer> findMaxDayNumberByQuoteId(@Param("quoteId") Long quoteId);

    void deleteByQuoteId(Long quoteId);
}
