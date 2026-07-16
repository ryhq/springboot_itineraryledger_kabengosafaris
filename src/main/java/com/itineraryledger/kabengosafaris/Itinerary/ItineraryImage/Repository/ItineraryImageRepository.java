package com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryImage.Entity.ItineraryImage.ImageType;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ItineraryImage entity.
 * Provides database operations for itinerary image management.
 */
@Repository
public interface ItineraryImageRepository extends JpaRepository<ItineraryImage, Long>, JpaSpecificationExecutor<ItineraryImage> {

    @Query("SELECT img FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId ORDER BY img.displayOrder ASC, img.createdAt DESC")
    List<ItineraryImage> findByItineraryIdOrderByDisplayOrderAsc(@Param("itineraryId") Long itineraryId);

    @Query("SELECT img FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId AND img.isActive = true ORDER BY img.displayOrder ASC, img.createdAt DESC")
    List<ItineraryImage> findActiveByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("SELECT img FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId AND img.imageType = :imageType AND img.isActive = true ORDER BY img.displayOrder ASC")
    List<ItineraryImage> findByItineraryIdAndImageType(@Param("itineraryId") Long itineraryId, @Param("imageType") ImageType imageType);

    @Query("SELECT img FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId AND img.isPrimary = true AND img.isActive = true")
    Optional<ItineraryImage> findPrimaryByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("SELECT COUNT(img) FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId")
    long countByItineraryId(@Param("itineraryId") Long itineraryId);

    @Query("SELECT COUNT(img) FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId AND img.isActive = true")
    long countActiveByItineraryId(@Param("itineraryId") Long itineraryId);

    boolean existsByFileName(String fileName);

    Optional<ItineraryImage> findByFileName(String fileName);

    @Query("SELECT COALESCE(MAX(img.displayOrder), 0) FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId")
    Integer findMaxDisplayOrderByItineraryId(@Param("itineraryId") Long itineraryId);

    @Modifying
    @Query("UPDATE ItineraryImage img SET img.isPrimary = false WHERE img.itinerary.id = :itineraryId")
    void unsetPrimaryForItinerary(@Param("itineraryId") Long itineraryId);

    @Query("SELECT img FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId")
    Page<ItineraryImage> findByItineraryIdPaginated(@Param("itineraryId") Long itineraryId, Pageable pageable);

    @Query("SELECT COUNT(img) > 0 FROM ItineraryImage img WHERE img.itinerary.id = :itineraryId AND img.isPrimary = true AND img.isActive = true")
    boolean hasPrimaryImage(@Param("itineraryId") Long itineraryId);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT img.id FROM ItineraryImage img WHERE img.id > :currentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT img.id FROM ItineraryImage img WHERE img.id < :currentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT img.id FROM ItineraryImage img ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT img.id FROM ItineraryImage img ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT img.id FROM ItineraryImage img WHERE img.id > :currentId AND img.itinerary.id = :parentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT img.id FROM ItineraryImage img WHERE img.id < :currentId AND img.itinerary.id = :parentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT img.id FROM ItineraryImage img WHERE img.itinerary.id = :parentId ORDER BY img.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT img.id FROM ItineraryImage img WHERE img.itinerary.id = :parentId ORDER BY img.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);
}
