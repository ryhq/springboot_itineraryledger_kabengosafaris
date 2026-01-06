package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationRoomTypeRepository extends JpaRepository<AccommodationRoomType, Long>, JpaSpecificationExecutor<AccommodationRoomType> {

    /**
     * Find room type by accommodation ID and name
     */
    Optional<AccommodationRoomType> findByAccommodationIdAndName(Long accommodationId, String name);

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
}
