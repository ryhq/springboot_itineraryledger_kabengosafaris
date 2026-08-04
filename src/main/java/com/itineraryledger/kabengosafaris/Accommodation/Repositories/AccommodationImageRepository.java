package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage.ImageType;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AccommodationImage entity.
 * Provides database operations for accommodation image management.
 */
@Repository
public interface AccommodationImageRepository extends JpaRepository<AccommodationImage, Long>, JpaSpecificationExecutor<AccommodationImage> {

    /**
     * Find all images for an accommodation
     */
    @Query("SELECT img FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId ORDER BY img.displayOrder ASC, img.createdAt DESC")
    List<AccommodationImage> findByAccommodationIdOrderByDisplayOrderAsc(@Param("accommodationId") Long accommodationId);

    /**
     * Find all active images for an accommodation
     */
    @Query("SELECT img FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId AND img.isActive = true ORDER BY img.displayOrder ASC, img.createdAt DESC")
    List<AccommodationImage> findActiveByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Find images by accommodation and type
     */
    @Query("SELECT img FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId AND img.imageType = :imageType AND img.isActive = true ORDER BY img.displayOrder ASC")
    List<AccommodationImage> findByAccommodationIdAndImageType(@Param("accommodationId") Long accommodationId, @Param("imageType") ImageType imageType);

    /**
     * Find the primary image for an accommodation
     */
    @Query("SELECT img FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId AND img.isPrimary = true AND img.isActive = true")
    Optional<AccommodationImage> findPrimaryByAccommodationId(@Param("accommodationId") Long accommodationId);
    /** PUBLIC: the primary image only if it is itself published. */
    @Query("SELECT img FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId AND img.isPrimary = true AND img.isActive = true AND img.isWebActive = true")
    Optional<AccommodationImage> findPublishedPrimaryByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Count images for an accommodation
     */
    @Query("SELECT COUNT(img) FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId")
    long countByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Count active images for an accommodation
     */
    @Query("SELECT COUNT(img) FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId AND img.isActive = true")
    long countActiveByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Check if filename exists
     */
    boolean existsByFileName(String fileName);

    /**
     * Find image by filename
     */
    Optional<AccommodationImage> findByFileName(String fileName);

    /**
     * Get max display order for an accommodation
     */
    @Query("SELECT COALESCE(MAX(img.displayOrder), 0) FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId")
    Integer findMaxDisplayOrderByAccommodationId(@Param("accommodationId") Long accommodationId);

    /**
     * Unset primary flag for all images of an accommodation
     */
    @Modifying
    @Query("UPDATE AccommodationImage img SET img.isPrimary = false WHERE img.accommodation.id = :accommodationId")
    void unsetPrimaryForAccommodation(@Param("accommodationId") Long accommodationId);

    /**
     * Find images with pagination
     */
    @Query("SELECT img FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId")
    Page<AccommodationImage> findByAccommodationIdPaginated(@Param("accommodationId") Long accommodationId, Pageable pageable);

    /**
     * The PUBLIC gallery query. The website was using the unfiltered one, so an
     * inactive or unpublished photo still reached the site.
     */
    @Query("SELECT img FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId AND img.isActive = true AND img.isWebActive = true ORDER BY img.displayOrder ASC, img.createdAt DESC")
    Page<AccommodationImage> findPublishedByAccommodationIdPaginated(@Param("accommodationId") Long accommodationId, Pageable pageable);

    /**
     * Check if accommodation has a primary image
     */
    @Query("SELECT COUNT(img) > 0 FROM AccommodationImage img WHERE img.accommodation.id = :accommodationId AND img.isPrimary = true AND img.isActive = true")
    boolean hasPrimaryImage(@Param("accommodationId") Long accommodationId);

    /**
     * Find all images by type across all accommodations (for admin/reporting)
     */
    @Query("SELECT img FROM AccommodationImage img WHERE img.imageType = :imageType AND img.isActive = true ORDER BY img.createdAt DESC")
    List<AccommodationImage> findAllByImageType(@Param("imageType") ImageType imageType);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT img.id FROM AccommodationImage img WHERE img.id > :currentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT img.id FROM AccommodationImage img WHERE img.id < :currentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT img.id FROM AccommodationImage img ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT img.id FROM AccommodationImage img ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT img.id FROM AccommodationImage img WHERE img.id > :currentId AND img.accommodation.id = :parentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT img.id FROM AccommodationImage img WHERE img.id < :currentId AND img.accommodation.id = :parentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT img.id FROM AccommodationImage img WHERE img.accommodation.id = :parentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT img.id FROM AccommodationImage img WHERE img.accommodation.id = :parentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);

    /**
     * Find all active images across all active accommodations (for gallery)
     */
    @Query("SELECT img FROM AccommodationImage img JOIN img.accommodation a WHERE img.isActive = true AND img.isWebActive = true AND a.isActive = true AND a.isWebActive = true ORDER BY img.createdAt DESC")
    Page<AccommodationImage> findAllActiveForGallery(Pageable pageable);
}
