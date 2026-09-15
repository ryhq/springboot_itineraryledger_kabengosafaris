package com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuoteInclusion.Entity.QuoteInclusion;

@Repository
public interface QuoteInclusionRepository extends JpaRepository<QuoteInclusion, Long> {

    List<QuoteInclusion> findByQuoteIdOrderBySortOrderAscIdAsc(Long quoteId);

    void deleteByQuoteId(Long quoteId);

    boolean existsByQuoteId(Long quoteId);
}
