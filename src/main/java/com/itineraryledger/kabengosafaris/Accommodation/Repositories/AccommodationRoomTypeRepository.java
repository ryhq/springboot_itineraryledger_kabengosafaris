package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccommodationRoomTypeRepository extends JpaRepository<AccommodationRoomType, Long>, JpaSpecificationExecutor<AccommodationRoomType> {

    /**
     * Find room type by accommodation ID and name
     */
    Optional<AccommodationRoomType> findByAccommodationIdAndName(Long accommodationId, String name);

    /** Every one of a lodge's room types, for reading it whole — export, and a rate's parents on import. */
    java.util.List<AccommodationRoomType> findByAccommodationId(Long accommodationId);

    /**
     * Check if room type exists by accommodation ID and name
     */
    @Query("SELECT COUNT(r) > 0 FROM AccommodationRoomType r WHERE r.accommodation.id = :accommodationId AND r.name = :name")
    boolean existsByAccommodationIdAndName(@Param("accommodationId") Long accommodationId, @Param("name") String name);

    /**
     * Check if room type exists by accommodation ID and name, excluding specific ID
     */
    @Query("SELECT COUNT(r) > 0 FROM AccommodationRoomType r WHERE r.accommodation.id = :accommodationId AND r.name = :name AND r.id != :excludeId")
    boolean existsByAccommodationIdAndNameExcludingId(@Param("accommodationId") Long accommodationId, @Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * Find unique room types based on name (grouped by name)
     * Returns one room type per unique name, sorted alphabetically
     */
    @Query("""
        SELECT rt FROM AccommodationRoomType rt
        WHERE rt.id IN (
            SELECT MIN(rt2.id)
            FROM AccommodationRoomType rt2
            WHERE rt2.isActive = true
            GROUP BY rt2.name
        )
        ORDER BY rt.name ASC
        """)
    List<AccommodationRoomType> findUniqueRoomTypesByName();

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT r.id FROM AccommodationRoomType r WHERE r.id > :currentId ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT r.id FROM AccommodationRoomType r WHERE r.id < :currentId ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT r.id FROM AccommodationRoomType r ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT r.id FROM AccommodationRoomType r ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // SCOPED NAVIGATION QUERIES (parent-scoped next/previous)
    // ========================

    @Query("SELECT r.id FROM AccommodationRoomType r WHERE r.id > :currentId AND r.accommodation.id = :parentId ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findNextIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT r.id FROM AccommodationRoomType r WHERE r.id < :currentId AND r.accommodation.id = :parentId ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByParent(@Param("currentId") Long currentId, @Param("parentId") Long parentId);

    @Query("SELECT r.id FROM AccommodationRoomType r WHERE r.accommodation.id = :parentId ORDER BY r.id ASC LIMIT 1")
    Optional<Long> findFirstIdByParent(@Param("parentId") Long parentId);

    @Query("SELECT r.id FROM AccommodationRoomType r WHERE r.accommodation.id = :parentId ORDER BY r.id DESC LIMIT 1")
    Optional<Long> findLastIdByParent(@Param("parentId") Long parentId);
}
