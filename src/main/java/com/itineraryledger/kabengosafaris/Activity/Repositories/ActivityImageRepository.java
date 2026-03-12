package com.itineraryledger.kabengosafaris.Activity.Repositories;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ActivityImage entity operations.
 */
@Repository
public interface ActivityImageRepository extends JpaRepository<ActivityImage, Long>, JpaSpecificationExecutor<ActivityImage> {

    List<ActivityImage> findByActivityIdOrderByDisplayOrderAsc(Long activityId);

    List<ActivityImage> findByActivityIdAndIsActiveOrderByDisplayOrderAsc(Long activityId, Boolean isActive);

    Optional<ActivityImage> findByFileName(String fileName);

    Optional<ActivityImage> findByActivityIdAndIsPrimaryTrue(Long activityId);

    @Query("SELECT COALESCE(MAX(ai.displayOrder), 0) FROM ActivityImage ai WHERE ai.activity.id = :activityId")
    int findMaxDisplayOrderByActivityId(@Param("activityId") Long activityId);

    @Modifying
    @Query("UPDATE ActivityImage ai SET ai.isPrimary = false WHERE ai.activity.id = :activityId AND ai.isPrimary = true")
    void unsetPrimaryForActivity(@Param("activityId") Long activityId);

    @Query("SELECT COUNT(ai) FROM ActivityImage ai WHERE ai.activity.id = :activityId")
    long countByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT COUNT(ai) FROM ActivityImage ai WHERE ai.activity.id = :activityId AND ai.isActive = true")
    long countActiveByActivityId(@Param("activityId") Long activityId);

    /**
     * Find active images with pagination
     */
    @Query("SELECT ai FROM ActivityImage ai WHERE ai.activity.id = :activityId AND ai.isActive = true ORDER BY ai.displayOrder ASC, ai.createdAt DESC")
    Page<ActivityImage> findActiveByActivityIdPaginated(@Param("activityId") Long activityId, Pageable pageable);

    boolean existsByActivityIdAndFileName(Long activityId, String fileName);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT ai.id FROM ActivityImage ai WHERE ai.id > :currentId ORDER BY ai.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT ai.id FROM ActivityImage ai WHERE ai.id < :currentId ORDER BY ai.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT ai.id FROM ActivityImage ai ORDER BY ai.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT ai.id FROM ActivityImage ai ORDER BY ai.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT ai.id FROM ActivityImage ai WHERE ai.id > :currentId AND ai.activity.id = :parentId ORDER BY ai.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT ai.id FROM ActivityImage ai WHERE ai.id < :currentId AND ai.activity.id = :parentId ORDER BY ai.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT ai.id FROM ActivityImage ai WHERE ai.activity.id = :parentId ORDER BY ai.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT ai.id FROM ActivityImage ai WHERE ai.activity.id = :parentId ORDER BY ai.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);
}
