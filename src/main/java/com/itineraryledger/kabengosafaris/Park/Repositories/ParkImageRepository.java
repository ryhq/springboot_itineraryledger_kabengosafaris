package com.itineraryledger.kabengosafaris.Park.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage.ImageType;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkImage entity.
 * Provides database operations for park image management.
 */
@Repository
public interface ParkImageRepository extends JpaRepository<ParkImage, Long>, JpaSpecificationExecutor<ParkImage> {

    /**
     * Find all images for a park
     */
    @Query("SELECT img FROM ParkImage img WHERE img.park.id = :parkId ORDER BY img.displayOrder ASC, img.createdAt DESC")
    List<ParkImage> findByParkIdOrderByDisplayOrderAsc(@Param("parkId") Long parkId);

    /**
     * Find all active images for a park
     */
    @Query("SELECT img FROM ParkImage img WHERE img.park.id = :parkId AND img.isActive = true AND img.isWebActive = true ORDER BY img.displayOrder ASC, img.createdAt DESC")
    List<ParkImage> findActiveByParkId(@Param("parkId") Long parkId);

    /**
     * Find images by park and type
     */
    @Query("SELECT img FROM ParkImage img WHERE img.park.id = :parkId AND img.imageType = :imageType AND img.isActive = true ORDER BY img.displayOrder ASC")
    List<ParkImage> findByParkIdAndImageType(@Param("parkId") Long parkId, @Param("imageType") ImageType imageType);

    /**
     * Find the primary image for a park
     */
    @Query("SELECT img FROM ParkImage img WHERE img.park.id = :parkId AND img.isPrimary = true AND img.isActive = true")
    Optional<ParkImage> findPrimaryByParkId(@Param("parkId") Long parkId);
    /** PUBLIC: the primary image only if it is itself published. */
    @Query("SELECT img FROM ParkImage img WHERE img.park.id = :parkId AND img.isPrimary = true AND img.isActive = true AND img.isWebActive = true")
    Optional<ParkImage> findPublishedPrimaryByParkId(@Param("parkId") Long parkId);

    /**
     * Count images for a park
     */
    @Query("SELECT COUNT(img) FROM ParkImage img WHERE img.park.id = :parkId")
    long countByParkId(@Param("parkId") Long parkId);

    /**
     * Count active images for a park
     */
    @Query("SELECT COUNT(img) FROM ParkImage img WHERE img.park.id = :parkId AND img.isActive = true")
    long countActiveByParkId(@Param("parkId") Long parkId);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Find image by filename
     */
    Optional<ParkImage> findByFileName(String fileName);

    /**
     * Get max display order for a park
     */
    @Query("SELECT COALESCE(MAX(img.displayOrder), 0) FROM ParkImage img WHERE img.park.id = :parkId")
    Integer findMaxDisplayOrderByParkId(@Param("parkId") Long parkId);

    /**
     * Unset primary flag for all images of a park
     */
    @Modifying
    @Query("UPDATE ParkImage img SET img.isPrimary = false WHERE img.park.id = :parkId")
    void unsetPrimaryForPark(@Param("parkId") Long parkId);

    /**
     * Find images with pagination
     */
    @Query("SELECT img FROM ParkImage img WHERE img.park.id = :parkId")
    Page<ParkImage> findByParkIdPaginated(@Param("parkId") Long parkId, Pageable pageable);

    /**
     * The PUBLIC gallery query: active AND published. isActive keeps a photo usable
     * in the panel; isWebActive decides whether the website may show it.
     */
    @Query("SELECT img FROM ParkImage img WHERE img.park.id = :parkId AND img.isActive = true ORDER BY img.displayOrder ASC, img.createdAt DESC")
    Page<ParkImage> findActiveByParkIdPaginated(@Param("parkId") Long parkId, Pageable pageable);

    /**
     * Check if park has a primary image
     */
    @Query("SELECT COUNT(img) > 0 FROM ParkImage img WHERE img.park.id = :parkId AND img.isPrimary = true AND img.isActive = true")
    boolean hasPrimaryImage(@Param("parkId") Long parkId);

    /**
     * Find all images by type across all parks (for admin/reporting)
     */
    @Query("SELECT img FROM ParkImage img WHERE img.imageType = :imageType AND img.isActive = true ORDER BY img.createdAt DESC")
    List<ParkImage> findAllByImageType(@Param("imageType") ImageType imageType);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT img.id FROM ParkImage img WHERE img.id > :currentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT img.id FROM ParkImage img WHERE img.id < :currentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT img.id FROM ParkImage img ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT img.id FROM ParkImage img ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT img.id FROM ParkImage img WHERE img.id > :currentId AND img.park.id = :parentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT img.id FROM ParkImage img WHERE img.id < :currentId AND img.park.id = :parentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT img.id FROM ParkImage img WHERE img.park.id = :parentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT img.id FROM ParkImage img WHERE img.park.id = :parentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);

    /**
     * Find all active images across all active parks (for gallery)
     */
    @Query("SELECT img FROM ParkImage img JOIN img.park p WHERE img.isActive = true AND img.isWebActive = true AND p.isActive = true AND p.isWebActive = true ORDER BY img.createdAt DESC")
    Page<ParkImage> findAllActiveForGallery(Pageable pageable);
}
