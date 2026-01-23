package com.itineraryledger.kabengosafaris.Customer.Repository;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * CustomerDocumentRepository - Repository for CustomerDocument entity
 */
@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {

    List<CustomerDocument> findByCustomerId(Long customerId);

    List<CustomerDocument> findByCustomerIdAndIsActiveTrue(Long customerId);

    List<CustomerDocument> findByCustomerIdAndDocumentType(Long customerId, String documentType);

    List<CustomerDocument> findByCustomerIdAndIsVerifiedFalse(Long customerId);

    @Query("SELECT d FROM CustomerDocument d WHERE d.customer.id = :customerId AND d.expiryDate < :date AND d.isActive = true")
    List<CustomerDocument> findExpiredDocuments(@Param("customerId") Long customerId, @Param("date") LocalDate date);

    @Query("SELECT d FROM CustomerDocument d WHERE d.customer.id = :customerId AND d.expiryDate BETWEEN :startDate AND :endDate AND d.isActive = true")
    List<CustomerDocument> findDocumentsExpiringSoon(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    void deleteByCustomerId(Long customerId);
}
