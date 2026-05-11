package com.itineraryledger.kabengosafaris.Quote.Repository;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    /**
     * Find quotes that are past their validity date and still in an expirable status (READY or SENT)
     */
    @Query("SELECT q FROM Quote q WHERE q.validTo < :today AND q.status IN :statuses")
    List<Quote> findExpiredQuotes(@Param("today") LocalDate today, @Param("statuses") List<QuoteStatus> statuses);

    /**
     * Find the latest quote for a given itinerary and customer,
     * preferring ACCEPTED/CONVERTED status, ordered by version descending.
     */
    @Query("SELECT q FROM Quote q WHERE q.itinerary.id = :itineraryId AND q.customer.id = :customerId " +
           "ORDER BY CASE q.status " +
           "WHEN 'CONVERTED' THEN 0 WHEN 'ACCEPTED' THEN 1 WHEN 'SENT' THEN 2 ELSE 3 END ASC, " +
           "q.version DESC")
    List<Quote> findByItineraryAndCustomerOrdered(@Param("itineraryId") Long itineraryId,
                                                   @Param("customerId") Long customerId);

    // Navigation queries for next/previous
    @Query("SELECT e.id FROM Quote e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Quote e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Quote e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Quote e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();

    /**
     * Null out previous_version_id / next_version_id columns on any quote
     * that points at the given quote, so the target can be safely deleted
     * without tripping the self-referential foreign key constraint.
     */
    @Modifying
    @Query("UPDATE Quote q SET q.previousVersion = null WHERE q.previousVersion.id = :id")
    int clearPreviousVersionRefs(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Quote q SET q.nextVersion = null WHERE q.nextVersion.id = :id")
    int clearNextVersionRefs(@Param("id") Long id);
}
