package com.itineraryledger.kabengosafaris.Quotation.Repository;

import com.itineraryledger.kabengosafaris.Quotation.Entity.Quotation;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * QuotationRepository - Data access layer for Quotation entities
 */
@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long>, JpaSpecificationExecutor<Quotation> {

    /**
     * Find quotation by code
     */
    Optional<Quotation> findByCode(String code);

    /**
     * Check if code exists
     */
    boolean existsByCode(String code);

    /**
     * Find all quotations for a customer
     */
    Page<Quotation> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Find all quotations from a specific itinerary template
     */
    Page<Quotation> findByItineraryId(Long itineraryId, Pageable pageable);

    /**
     * Find all quotations by status
     */
    Page<Quotation> findByStatus(QuotationStatus status, Pageable pageable);

    /**
     * Find all quotations assigned to a specific user
     */
    Page<Quotation> findByAssignedTo(Long userId, Pageable pageable);

    /**
     * Find all revisions of a quotation (child quotations)
     */
    List<Quotation> findByParentQuotationIdOrderByVersionDesc(Long parentQuotationId);

    /**
     * Find the latest version of a quotation chain
     */
    @Query("SELECT q FROM Quotation q WHERE q.parentQuotation.id = :parentId ORDER BY q.version DESC LIMIT 1")
    Optional<Quotation> findLatestRevision(@Param("parentId") Long parentQuotationId);

    /**
     * Find quotations expiring soon (within specified days)
     */
    @Query("SELECT q FROM Quotation q WHERE q.status IN ('SENT', 'VIEWED') AND q.validUntil <= :expiryDate AND q.validUntil >= :today")
    Page<Quotation> findExpiringSoon(@Param("today") LocalDate today, @Param("expiryDate") LocalDate expiryDate, Pageable pageable);

    /**
     * Find expired quotations that haven't been marked as expired yet
     */
    @Query("SELECT q FROM Quotation q WHERE q.status IN ('SENT', 'VIEWED') AND q.validUntil < :today")
    List<Quotation> findExpiredQuotations(@Param("today") LocalDate today);

    /**
     * Mark expired quotations as expired
     */
    @Modifying
    @Query("UPDATE Quotation q SET q.status = 'EXPIRED', q.updatedAt = :now WHERE q.status IN ('SENT', 'VIEWED') AND q.validUntil < :today")
    int markExpiredQuotations(@Param("today") LocalDate today, @Param("now") LocalDateTime now);

    /**
     * Find quotations by customer and status
     */
    Page<Quotation> findByCustomerIdAndStatus(Long customerId, QuotationStatus status, Pageable pageable);

    /**
     * Count quotations by customer
     */
    long countByCustomerId(Long customerId);

    /**
     * Count quotations by status
     */
    long countByStatus(QuotationStatus status);

    /**
     * Count quotations assigned to user
     */
    long countByAssignedTo(Long userId);

    /**
     * Find quotations created within date range
     */
    @Query("SELECT q FROM Quotation q WHERE q.createdAt >= :startDate AND q.createdAt <= :endDate")
    Page<Quotation> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Find quotations with start date in range
     */
    Page<Quotation> findByStartDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Get conversion rate (accepted quotations / total sent quotations)
     */
    @Query("SELECT COUNT(q) FROM Quotation q WHERE q.status = 'ACCEPTED' AND q.createdAt >= :startDate")
    long countAcceptedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(q) FROM Quotation q WHERE q.status IN ('SENT', 'VIEWED', 'ACCEPTED', 'REJECTED', 'EXPIRED') AND q.createdAt >= :startDate")
    long countSentSince(@Param("startDate") LocalDateTime startDate);

    /**
     * Find quotations that need follow-up (sent but not viewed for X days)
     */
    @Query("SELECT q FROM Quotation q WHERE q.status = 'SENT' AND q.sentAt < :cutoffDate")
    Page<Quotation> findNeedingFollowUp(@Param("cutoffDate") LocalDateTime cutoffDate, Pageable pageable);
}
