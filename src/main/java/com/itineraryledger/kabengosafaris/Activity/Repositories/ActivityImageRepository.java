package com.itineraryledger.kabengosafaris.Activity.Repositories;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
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

    boolean existsByActivityIdAndFileName(Long activityId, String fileName);
}
