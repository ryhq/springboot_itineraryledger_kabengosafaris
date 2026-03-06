package com.itineraryledger.kabengosafaris.Customer.Repository;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerPhone.PhoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CustomerPhoneRepository - Repository for CustomerPhone entity
 */
@Repository
public interface CustomerPhoneRepository extends JpaRepository<CustomerPhone, Long>, JpaSpecificationExecutor<CustomerPhone> {

    List<CustomerPhone> findByCustomerId(Long customerId);

    List<CustomerPhone> findByCustomerIdAndIsActiveTrue(Long customerId);

    Optional<CustomerPhone> findByCustomerIdAndIsPrimaryTrue(Long customerId);

    List<CustomerPhone> findByCustomerIdAndPhoneType(Long customerId, PhoneType phoneType);

    List<CustomerPhone> findByCustomerIdAndIsWhatsAppTrue(Long customerId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    void deleteByCustomerId(Long customerId);

    /**
     * Mark all phones for a customer as non-primary
     */
    @Modifying
    @Query("UPDATE CustomerPhone p SET p.isPrimary = false WHERE p.customer.id = :customerId")
    void markAllAsNonPrimaryForCustomer(@Param("customerId") Long customerId);

    /**
     * Mark all phones for a customer except one as non-primary
     */
    @Modifying
    @Query("UPDATE CustomerPhone p SET p.isPrimary = false WHERE p.customer.id = :customerId AND p.id != :excludePhoneId")
    void markAllAsNonPrimaryExcept(@Param("customerId") Long customerId, @Param("excludePhoneId") Long excludePhoneId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT p.id FROM CustomerPhone p WHERE p.id > :currentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT p.id FROM CustomerPhone p WHERE p.id < :currentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT p.id FROM CustomerPhone p ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT p.id FROM CustomerPhone p ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
