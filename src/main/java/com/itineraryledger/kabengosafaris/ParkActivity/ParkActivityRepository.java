package com.itineraryledger.kabengosafaris.ParkActivity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkActivity join entity
 */
@Repository
public interface ParkActivityRepository extends JpaRepository<ParkActivity, ParkActivityId> {

    /**
     * Find all park-activity associations for a specific park
     */
    List<ParkActivity> findByParkId(Long parkId);

    /**
     * Find all park-activity associations for a specific activity
     */
    List<ParkActivity> findByActivityId(Long activityId);

    /**
     * Check if a specific park-activity association exists
     */
    boolean existsByParkIdAndActivityId(Long parkId, Long activityId);

    /**
     * Find a specific park-activity association
     */
    Optional<ParkActivity> findByParkIdAndActivityId(Long parkId, Long activityId);

    /**
     * Delete all associations for a specific park
     */
    void deleteByParkId(Long parkId);

    /**
     * Delete all associations for a specific activity
     */
    void deleteByActivityId(Long activityId);

    /**
     * Delete a specific park-activity association
     */
    void deleteByParkIdAndActivityId(Long parkId, Long activityId);

    /**
     * Count activities for a specific park
     */
    long countByParkId(Long parkId);

    /**
     * Count parks for a specific activity
     */
    long countByActivityId(Long activityId);

    /**
     * Get all activities for a park (eager fetch)
     */
    @Query("SELECT pa FROM ParkActivity pa JOIN FETCH pa.activity WHERE pa.park.id = :parkId")
    List<ParkActivity> findByParkIdWithActivity(@Param("parkId") Long parkId);

    /**
     * Get all parks for an activity (eager fetch)
     */
    @Query("SELECT pa FROM ParkActivity pa JOIN FETCH pa.park WHERE pa.activity.id = :activityId")
    List<ParkActivity> findByActivityIdWithPark(@Param("activityId") Long activityId);
}
