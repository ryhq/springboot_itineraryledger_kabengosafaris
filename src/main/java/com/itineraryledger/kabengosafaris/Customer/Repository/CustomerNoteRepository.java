package com.itineraryledger.kabengosafaris.Customer.Repository;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CustomerNoteRepository - Repository for CustomerNote entity
 */
@Repository
public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long>, JpaSpecificationExecutor<CustomerNote> {

    List<CustomerNote> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<CustomerNote> findByCustomerId(Long customerId, Pageable pageable);

    List<CustomerNote> findByCustomerIdAndNoteType(Long customerId, String noteType);

    List<CustomerNote> findByCustomerIdAndIsPinnedTrue(Long customerId);

    @Query("SELECT n FROM CustomerNote n WHERE n.customer.id = :customerId AND n.followUpDate IS NOT NULL AND n.followUpCompleted = false ORDER BY n.followUpDate ASC")
    List<CustomerNote> findPendingFollowUps(@Param("customerId") Long customerId);

    @Query("SELECT n FROM CustomerNote n WHERE n.followUpDate < :date AND n.followUpCompleted = false")
    List<CustomerNote> findOverdueFollowUps(@Param("date") LocalDateTime date);

    @Query("SELECT n FROM CustomerNote n WHERE n.followUpDate BETWEEN :startDate AND :endDate AND n.followUpCompleted = false")
    List<CustomerNote> findUpcomingFollowUps(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByCustomerId(Long customerId);

    void deleteByCustomerId(Long customerId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT n.id FROM CustomerNote n WHERE n.id > :currentId ORDER BY n.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT n.id FROM CustomerNote n WHERE n.id < :currentId ORDER BY n.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT n.id FROM CustomerNote n ORDER BY n.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT n.id FROM CustomerNote n ORDER BY n.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT n.id FROM CustomerNote n WHERE n.id > :currentId AND n.customer.id = :parentId ORDER BY n.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT n.id FROM CustomerNote n WHERE n.id < :currentId AND n.customer.id = :parentId ORDER BY n.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT n.id FROM CustomerNote n WHERE n.customer.id = :parentId ORDER BY n.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT n.id FROM CustomerNote n WHERE n.customer.id = :parentId ORDER BY n.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);
}
