package com.itineraryledger.kabengosafaris.Customer.Repository;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerEmail.EmailType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CustomerEmailRepository - Repository for CustomerEmail entity
 */
@Repository
public interface CustomerEmailRepository extends JpaRepository<CustomerEmail, Long>, JpaSpecificationExecutor<CustomerEmail> {

    List<CustomerEmail> findByCustomerId(Long customerId);

    List<CustomerEmail> findByCustomerIdAndIsActiveTrue(Long customerId);

    Optional<CustomerEmail> findByCustomerIdAndIsPrimaryTrue(Long customerId);

    List<CustomerEmail> findByCustomerIdAndEmailType(Long customerId, EmailType emailType);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<CustomerEmail> findByEmail(String email);

    void deleteByCustomerId(Long customerId);

    /**
     * Mark all emails for a customer as non-primary
     */
    @Modifying
    @Query("UPDATE CustomerEmail e SET e.isPrimary = false WHERE e.customer.id = :customerId")
    void markAllAsNonPrimaryForCustomer(@Param("customerId") Long customerId);

    /**
     * Mark all emails for a customer except one as non-primary
     */
    @Modifying
    @Query("UPDATE CustomerEmail e SET e.isPrimary = false WHERE e.customer.id = :customerId AND e.id != :excludeEmailId")
    void markAllAsNonPrimaryExcept(@Param("customerId") Long customerId, @Param("excludeEmailId") Long excludeEmailId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT e.id FROM CustomerEmail e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM CustomerEmail e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM CustomerEmail e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM CustomerEmail e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT e.id FROM CustomerEmail e WHERE e.id > :currentId AND e.customer.id = :parentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT e.id FROM CustomerEmail e WHERE e.id < :currentId AND e.customer.id = :parentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT e.id FROM CustomerEmail e WHERE e.customer.id = :parentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT e.id FROM CustomerEmail e WHERE e.customer.id = :parentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);
}
