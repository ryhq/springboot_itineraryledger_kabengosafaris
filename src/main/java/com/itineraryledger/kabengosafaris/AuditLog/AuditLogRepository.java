package com.itineraryledger.kabengosafaris.AuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE YEAR(a.createdAt) = :year AND MONTH(a.createdAt) = :month AND a.name NOT LIKE 'TEMP_%'")
    long countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    boolean existsByName(String name);

    // ========================
    // CIRCULAR NAVIGATION QUERIES (global scope, ordered by id)
    // ========================

    @Query("SELECT a.id FROM AuditLog a WHERE a.id > :currentId ORDER BY a.id ASC LIMIT 1")
    java.util.Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT a.id FROM AuditLog a WHERE a.id < :currentId ORDER BY a.id DESC LIMIT 1")
    java.util.Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT a.id FROM AuditLog a ORDER BY a.id ASC LIMIT 1")
    java.util.Optional<Long> findFirstId();

    @Query("SELECT a.id FROM AuditLog a ORDER BY a.id DESC LIMIT 1")
    java.util.Optional<Long> findLastId();
}
