package com.itineraryledger.kabengosafaris.Quote.Repository;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for QuoteItem entity.
 * Provides database operations for quote item management.
 */
@Repository
public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long>, JpaSpecificationExecutor<QuoteItem> {

    /**
     * Find all items for a quote
     */
    @Query("SELECT item FROM QuoteItem item WHERE item.quote.id = :quoteId ORDER BY item.displayOrder ASC, item.createdAt ASC")
    List<QuoteItem> findByQuoteIdOrderByDisplayOrder(@Param("quoteId") Long quoteId);

    /**
     * Find all active items for a quote
     */
    @Query("SELECT item FROM QuoteItem item WHERE item.quote.id = :quoteId AND item.isActive = true ORDER BY item.displayOrder ASC, item.createdAt ASC")
    List<QuoteItem> findActiveByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Find items by quote and type
     */
    @Query("SELECT item FROM QuoteItem item WHERE item.quote.id = :quoteId AND item.itemType = :itemType AND item.isActive = true ORDER BY item.displayOrder ASC")
    List<QuoteItem> findByQuoteIdAndItemType(@Param("quoteId") Long quoteId, @Param("itemType") QuoteItemType itemType);

    /**
     * Count items for a quote
     */
    @Query("SELECT COUNT(item) FROM QuoteItem item WHERE item.quote.id = :quoteId")
    long countByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Count active items for a quote
     */
    @Query("SELECT COUNT(item) FROM QuoteItem item WHERE item.quote.id = :quoteId AND item.isActive = true")
    long countActiveByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Find maximum display order for a quote
     * Returns null if no items exist for the quote
     */
    @Query("SELECT MAX(item.displayOrder) FROM QuoteItem item WHERE item.quote.id = :quoteId")
    Integer findMaxDisplayOrderByQuoteId(@Param("quoteId") Long quoteId);

    /**
     * Delete all items for a quote
     */
    void deleteByQuoteId(Long quoteId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT q.id FROM QuoteItem q WHERE q.id > :currentId ORDER BY q.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT q.id FROM QuoteItem q WHERE q.id < :currentId ORDER BY q.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT q.id FROM QuoteItem q ORDER BY q.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT q.id FROM QuoteItem q ORDER BY q.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
