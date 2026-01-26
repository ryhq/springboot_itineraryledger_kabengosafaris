package com.itineraryledger.kabengosafaris.ParkActivity.Repositories;

import com.itineraryledger.kabengosafaris.ParkActivity.Entities.ParkActivityImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkActivityImage entity operations.
 * Queries use composite key (parkId + activityId) to identify park-activity relationships.
 */
@Repository
public interface ParkActivityImageRepository extends JpaRepository<ParkActivityImage, Long>, JpaSpecificationExecutor<ParkActivityImage> {

    /**
     * Find all images for a specific park-activity relationship
     */
    @Query("SELECT pai FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId " +
           "ORDER BY pai.displayOrder ASC")
    List<ParkActivityImage> findByParkActivityOrderByDisplayOrderAsc(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Find active images for a specific park-activity relationship
     */
    @Query("SELECT pai FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId " +
           "AND pai.isActive = :isActive " +
           "ORDER BY pai.displayOrder ASC")
    List<ParkActivityImage> findByParkActivityAndIsActiveOrderByDisplayOrderAsc(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId,
        @Param("isActive") Boolean isActive
    );

    /**
     * Find all images for a specific park
     */
    @Query("SELECT pai FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "ORDER BY pai.displayOrder ASC")
    List<ParkActivityImage> findByParkIdOrderByDisplayOrderAsc(@Param("parkId") Long parkId);

    /**
     * Find all images for a specific activity (across all parks)
     */
    @Query("SELECT pai FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.activity.id = :activityId " +
           "ORDER BY pai.displayOrder ASC")
    List<ParkActivityImage> findByActivityIdOrderByDisplayOrderAsc(@Param("activityId") Long activityId);

    /**
     * Find image by filename
     */
    Optional<ParkActivityImage> findByFileName(String fileName);

    /**
     * Find primary image for a park-activity relationship
     */
    @Query("SELECT pai FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId " +
           "AND pai.isPrimary = true")
    Optional<ParkActivityImage> findPrimaryByParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Get max display order for a park-activity relationship
     */
    @Query("SELECT COALESCE(MAX(pai.displayOrder), 0) FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId")
    int findMaxDisplayOrderByParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Unset primary flag for all images of a park-activity relationship
     */
    @Modifying
    @Query("UPDATE ParkActivityImage pai SET pai.isPrimary = false " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId " +
           "AND pai.isPrimary = true")
    void unsetPrimaryForParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Count images for a park-activity relationship
     */
    @Query("SELECT COUNT(pai) FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId")
    long countByParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Count active images for a park-activity relationship
     */
    @Query("SELECT COUNT(pai) FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId " +
           "AND pai.isActive = true")
    long countActiveByParkActivity(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId
    );

    /**
     * Check if an image with filename exists for a park-activity relationship
     */
    @Query("SELECT COUNT(pai) > 0 FROM ParkActivityImage pai " +
           "WHERE pai.parkActivity.park.id = :parkId " +
           "AND pai.parkActivity.activity.id = :activityId " +
           "AND pai.fileName = :fileName")
    boolean existsByParkActivityAndFileName(
        @Param("parkId") Long parkId,
        @Param("activityId") Long activityId,
        @Param("fileName") String fileName
    );
}
