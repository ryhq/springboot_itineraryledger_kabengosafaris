package com.itineraryledger.kabengosafaris.Quotation.Repository;

import com.itineraryledger.kabengosafaris.Quotation.Entity.QuotationPax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * QuotationPaxRepository - Data access layer for QuotationPax entities
 */
@Repository
public interface QuotationPaxRepository extends JpaRepository<QuotationPax, Long>, JpaSpecificationExecutor<QuotationPax> {

    /**
     * Find all pax configurations for a quotation
     */
    List<QuotationPax> findByQuotationIdOrderByIdAsc(Long quotationId);

    /**
     * Find pax by quotation and nation/age category combination
     */
    Optional<QuotationPax> findByQuotationIdAndNationCategoryIdAndAgeCategoryId(
        Long quotationId,
        Long nationCategoryId,
        Long ageCategoryId
    );

    /**
     * Check if pax combination exists
     */
    boolean existsByQuotationIdAndNationCategoryIdAndAgeCategoryId(
        Long quotationId,
        Long nationCategoryId,
        Long ageCategoryId
    );

    /**
     * Delete all pax configurations for a quotation
     */
    @Modifying
    @Query("DELETE FROM QuotationPax qp WHERE qp.quotation.id = :quotationId")
    void deleteAllByQuotationId(@Param("quotationId") Long quotationId);

    /**
     * Count pax configurations for a quotation
     */
    long countByQuotationId(Long quotationId);

    /**
     * Get total pax count for a quotation
     */
    @Query("SELECT COALESCE(SUM(qp.count), 0) FROM QuotationPax qp WHERE qp.quotation.id = :quotationId")
    int getTotalPaxCount(@Param("quotationId") Long quotationId);
}
