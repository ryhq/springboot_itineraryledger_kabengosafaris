package com.itineraryledger.kabengosafaris.Customer.Repository;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CustomerRepository - Repository for Customer entity
 *
 * Extends JpaSpecificationExecutor for dynamic filtering support.
 *
 * NOTE: Email-related methods have been removed since Customer no longer
 * has a direct email field. Use CustomerEmailRepository for email queries.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    // ========================
    // FIND BY CODE
    // ========================

    Optional<Customer> findByCode(String code);

    Optional<Customer> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    // ========================
    // FIND BY TYPE
    // ========================

    List<Customer> findByCustomerType(CustomerType customerType);

    long countByCustomerType(CustomerType customerType);

    // ========================
    // FIND BY STATUS
    // ========================

    List<Customer> findByIsActiveTrue();

    List<Customer> findByIsActiveFalse();

    List<Customer> findByIsVipTrue();

    List<Customer> findByIsBlacklistedTrue();

    long countByIsActiveTrue();

    long countByIsVipTrue();

    long countByIsBlacklistedTrue();

    // ========================
    // SEARCH QUERIES
    // ========================

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Customer> searchCustomers(@Param("search") String search);

    @Query("SELECT c FROM Customer c WHERE c.isActive = true AND " +
           "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Customer> searchActiveCustomers(@Param("search") String search);

    // ========================
    // NATIONALITY QUERIES
    // ========================

    List<Customer> findByNationality(String nationality);

    List<Customer> findByNationalityIgnoreCase(String nationality);

    // ========================
    // COUNTRY QUERIES
    // ========================

    List<Customer> findByCountry(String country);

    List<Customer> findByCountryIgnoreCase(String country);

    // ========================
    // TIME-BASED QUERIES
    // ========================

    /**
     * Count customers created after a specific date/time
     */
    long countByCreatedAtAfter(java.time.LocalDateTime createdAt);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT c.id FROM Customer c WHERE c.id > :currentId ORDER BY c.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT c.id FROM Customer c WHERE c.id < :currentId ORDER BY c.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT c.id FROM Customer c ORDER BY c.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT c.id FROM Customer c ORDER BY c.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
