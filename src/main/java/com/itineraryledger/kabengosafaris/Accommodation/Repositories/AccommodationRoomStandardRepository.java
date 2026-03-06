package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccommodationRoomStandardRepository extends JpaRepository<AccommodationRoomStandard, Long>, JpaSpecificationExecutor<AccommodationRoomStandard> {

    /**
     * Find room standard by accommodation ID and name
     */
    Optional<AccommodationRoomStandard> findByAccommodationIdAndName(Long accommodationId, String name);

    /**
     * Check if room standard exists by accommodation ID and name
     */
    @Query("SELECT COUNT(rs) > 0 FROM AccommodationRoomStandard rs WHERE rs.accommodation.id = :accommodationId AND rs.name = :name")
    boolean existsByAccommodationIdAndName(@Param("accommodationId") Long accommodationId, @Param("name") String name);

    /**
     * Check if room standard exists by accommodation ID and name, excluding specific ID
     */
    @Query("SELECT COUNT(rs) > 0 FROM AccommodationRoomStandard rs WHERE rs.accommodation.id = :accommodationId AND rs.name = :name AND rs.id != :excludeId")
    boolean existsByAccommodationIdAndNameExcludingId(@Param("accommodationId") Long accommodationId, @Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * Find unique room standards based on name (grouped by name)
     * Returns one room standard per unique name, sorted alphabetically
     */
    @Query("""
        SELECT rs FROM AccommodationRoomStandard rs
        WHERE rs.id IN (
            SELECT MIN(rs2.id)
            FROM AccommodationRoomStandard rs2
            WHERE rs2.isActive = true
            GROUP BY rs2.name
        )
        ORDER BY rs.name ASC
        """)
    List<AccommodationRoomStandard> findUniqueRoomStandardsByName();

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT rs.id FROM AccommodationRoomStandard rs WHERE rs.id > :currentId ORDER BY rs.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT rs.id FROM AccommodationRoomStandard rs WHERE rs.id < :currentId ORDER BY rs.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT rs.id FROM AccommodationRoomStandard rs ORDER BY rs.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT rs.id FROM AccommodationRoomStandard rs ORDER BY rs.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
