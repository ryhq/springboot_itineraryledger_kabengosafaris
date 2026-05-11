package com.itineraryledger.kabengosafaris.Quote.QuotePax.Repository;

import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotePaxRepository
        extends JpaRepository<QuotePax, Long>, JpaSpecificationExecutor<QuotePax> {

    List<QuotePax> findByQuoteIdOrderByIdAsc(Long quoteId);

    long countByQuoteId(Long quoteId);

    Optional<QuotePax> findByQuoteIdAndNationCategoryIdAndAgeCategoryId(
            Long quoteId, Long nationCategoryId, Long ageCategoryId);
}
