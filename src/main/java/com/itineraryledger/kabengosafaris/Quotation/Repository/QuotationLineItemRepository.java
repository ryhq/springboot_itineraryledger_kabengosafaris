package com.itineraryledger.kabengosafaris.Quotation.Repository;

import com.itineraryledger.kabengosafaris.Quotation.Entity.QuotationLineItem;
import com.itineraryledger.kabengosafaris.Quotation.Enums.LineItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * QuotationLineItemRepository - Data access layer for QuotationLineItem entities
 */
@Repository
public interface QuotationLineItemRepository extends JpaRepository<QuotationLineItem, Long>, JpaSpecificationExecutor<QuotationLineItem> {

    /**
     * Find all line items for a quotation ordered by day and sort order
     */
    List<QuotationLineItem> findByQuotationIdOrderByDayNumberAscSortOrderAsc(Long quotationId);

    /**
     * Find line items for a specific day
     */
    List<QuotationLineItem> findByQuotationIdAndDayNumberOrderBySortOrderAsc(Long quotationId, Integer dayNumber);

    /**
     * Find overall line items (not day-specific)
     */
    @Query("SELECT li FROM QuotationLineItem li WHERE li.quotation.id = :quotationId AND (li.dayNumber IS NULL OR li.dayNumber = 0) ORDER BY li.sortOrder ASC")
    List<QuotationLineItem> findOverallLineItems(@Param("quotationId") Long quotationId);

    /**
     * Find line items by type
     */
    List<QuotationLineItem> findByQuotationIdAndItemTypeOrderByDayNumberAscSortOrderAsc(Long quotationId, LineItemType itemType);

    /**
     * Find line items by reference
     */
    List<QuotationLineItem> findByQuotationIdAndReferenceTypeAndReferenceId(Long quotationId, String referenceType, Long referenceId);

    /**
     * Delete all line items for a quotation
     */
    @Modifying
    @Query("DELETE FROM QuotationLineItem li WHERE li.quotation.id = :quotationId")
    void deleteAllByQuotationId(@Param("quotationId") Long quotationId);

    /**
     * Delete line items for a specific day
     */
    @Modifying
    @Query("DELETE FROM QuotationLineItem li WHERE li.quotation.id = :quotationId AND li.dayNumber = :dayNumber")
    void deleteByQuotationIdAndDayNumber(@Param("quotationId") Long quotationId, @Param("dayNumber") Integer dayNumber);

    /**
     * Count line items for a quotation
     */
    long countByQuotationId(Long quotationId);

    /**
     * Get total amount for all line items in a quotation
     */
    @Query("SELECT COALESCE(SUM(li.totalPrice), 0) FROM QuotationLineItem li WHERE li.quotation.id = :quotationId AND li.isIncluded = true")
    BigDecimal getTotalAmountByQuotationId(@Param("quotationId") Long quotationId);

    /**
     * Get total amount by item type
     */
    @Query("SELECT COALESCE(SUM(li.totalPrice), 0) FROM QuotationLineItem li WHERE li.quotation.id = :quotationId AND li.itemType = :itemType AND li.isIncluded = true")
    BigDecimal getTotalAmountByItemType(@Param("quotationId") Long quotationId, @Param("itemType") LineItemType itemType);

    /**
     * Get the maximum sort order for a day
     */
    @Query("SELECT COALESCE(MAX(li.sortOrder), 0) FROM QuotationLineItem li WHERE li.quotation.id = :quotationId AND li.dayNumber = :dayNumber")
    int getMaxSortOrderForDay(@Param("quotationId") Long quotationId, @Param("dayNumber") Integer dayNumber);

    /**
     * Update sort orders after deletion
     */
    @Modifying
    @Query("UPDATE QuotationLineItem li SET li.sortOrder = li.sortOrder - 1 WHERE li.quotation.id = :quotationId AND li.dayNumber = :dayNumber AND li.sortOrder > :deletedSortOrder")
    void decrementSortOrdersAfter(@Param("quotationId") Long quotationId, @Param("dayNumber") Integer dayNumber, @Param("deletedSortOrder") Integer deletedSortOrder);
}
