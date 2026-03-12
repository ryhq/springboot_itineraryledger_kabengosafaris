package com.itineraryledger.kabengosafaris.Activity.Repositories;

import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityDocument.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ActivityDocument entity operations.
 */
@Repository
public interface ActivityDocumentRepository extends JpaRepository<ActivityDocument, Long>, JpaSpecificationExecutor<ActivityDocument> {

    List<ActivityDocument> findByActivityIdOrderByCreatedAtDesc(Long activityId);

    List<ActivityDocument> findByActivityIdAndIsActiveOrderByCreatedAtDesc(Long activityId, Boolean isActive);

    List<ActivityDocument> findByActivityIdAndDocumentTypeOrderByCreatedAtDesc(Long activityId, DocumentType documentType);

    Optional<ActivityDocument> findByFileName(String fileName);

    @Query("SELECT ad FROM ActivityDocument ad WHERE ad.activity.id = :activityId " +
           "AND ad.isActive = true " +
           "AND (ad.validFrom IS NULL OR ad.validFrom <= :date) " +
           "AND (ad.validTo IS NULL OR ad.validTo >= :date)")
    List<ActivityDocument> findCurrentlyValidByActivityId(@Param("activityId") Long activityId, @Param("date") LocalDateTime date);

    @Query("SELECT ad FROM ActivityDocument ad WHERE ad.activity.id = :activityId " +
           "AND ad.documentType IN :types " +
           "AND ad.isActive = true " +
           "ORDER BY ad.createdAt DESC")
    List<ActivityDocument> findByActivityIdAndDocumentTypes(
        @Param("activityId") Long activityId,
        @Param("types") List<DocumentType> types
    );

    @Query("SELECT COUNT(ad) FROM ActivityDocument ad WHERE ad.activity.id = :activityId")
    long countByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT COUNT(ad) FROM ActivityDocument ad WHERE ad.activity.id = :activityId AND ad.isActive = true")
    long countActiveByActivityId(@Param("activityId") Long activityId);

    boolean existsByActivityIdAndFileName(Long activityId, String fileName);

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT ad.id FROM ActivityDocument ad WHERE ad.id > :currentId ORDER BY ad.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT ad.id FROM ActivityDocument ad WHERE ad.id < :currentId ORDER BY ad.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT ad.id FROM ActivityDocument ad ORDER BY ad.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT ad.id FROM ActivityDocument ad ORDER BY ad.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT ad.id FROM ActivityDocument ad WHERE ad.id > :currentId AND ad.activity.id = :parentId ORDER BY ad.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT ad.id FROM ActivityDocument ad WHERE ad.id < :currentId AND ad.activity.id = :parentId ORDER BY ad.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT ad.id FROM ActivityDocument ad WHERE ad.activity.id = :parentId ORDER BY ad.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT ad.id FROM ActivityDocument ad WHERE ad.activity.id = :parentId ORDER BY ad.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);
}
