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
    // FILTER VALUES — what this log actually holds
    // ========================

    /*
     * DISTINCT over the real rows rather than a hard-coded list. There are 330 action names
     * and 87 entity types in the code and both grow with every module; a list written here
     * would drift, and a filter that offers a value matching nothing is worse than no
     * filter. NULLs are excluded so the dropdown never carries a blank entry.
     */
    @Query("SELECT DISTINCT a.action FROM AuditLog a WHERE a.action IS NOT NULL ORDER BY a.action ASC")
    java.util.List<String> distinctActions();

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a WHERE a.entityType IS NOT NULL ORDER BY a.entityType ASC")
    java.util.List<String> distinctEntityTypes();

    @Query("SELECT DISTINCT a.status FROM AuditLog a WHERE a.status IS NOT NULL ORDER BY a.status ASC")
    java.util.List<String> distinctStatuses();

    @Query("SELECT DISTINCT a.username FROM AuditLog a WHERE a.username IS NOT NULL ORDER BY a.username ASC")
    java.util.List<String> distinctUsernames();

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
