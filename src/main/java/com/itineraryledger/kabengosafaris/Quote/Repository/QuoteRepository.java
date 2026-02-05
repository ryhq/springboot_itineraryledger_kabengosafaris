package com.itineraryledger.kabengosafaris.Quote.Repository;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Quote entity.
 * Provides database operations for quote management.
 */
@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {

    /**
     * Find quote by quote code
     */
    Optional<Quote> findByQuoteCode(String quoteCode);

    /**
     * Find quote by quote code (case-insensitive)
     */
    Optional<Quote> findByQuoteCodeIgnoreCase(String quoteCode);

    /**
     * Check if quote code exists
     */
    boolean existsByQuoteCode(String quoteCode);

    /**
     * Check if quote code exists for a different quote
     */
    boolean existsByQuoteCodeAndIdNot(String quoteCode, Long id);

    /**
     * Count quotes by status
     */
    long countByStatus(QuoteStatus status);

    /**
     * Count active quotes
     */
    long countByIsActiveTrue();

    /**
     * Count quotes by customer
     */
    long countByCustomerId(Long customerId);

    /**
     * Count quotes by itinerary
     */
    long countByItineraryId(Long itineraryId);
}
