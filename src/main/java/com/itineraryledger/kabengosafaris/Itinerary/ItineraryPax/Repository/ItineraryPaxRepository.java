package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryPaxRepository extends JpaRepository<ItineraryPax, Long>, JpaSpecificationExecutor<ItineraryPax> {

    List<ItineraryPax> findByItineraryId(Long itineraryId);

    Optional<ItineraryPax> findByItineraryIdAndNationCategoryIdAndAgeCategoryId(
        Long itineraryId, Long nationCategoryId, Long ageCategoryId);

    boolean existsByItineraryIdAndNationCategoryIdAndAgeCategoryId(
        Long itineraryId, Long nationCategoryId, Long ageCategoryId);

    void deleteByItineraryId(Long itineraryId);

    long countByItineraryId(Long itineraryId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within itinerary)
    // ========================

    @Query("SELECT p.id FROM ItineraryPax p WHERE p.itinerary.id = :parentId AND p.id > :currentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM ItineraryPax p WHERE p.itinerary.id = :parentId AND p.id < :currentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM ItineraryPax p WHERE p.itinerary.id = :parentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT p.id FROM ItineraryPax p WHERE p.itinerary.id = :parentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
