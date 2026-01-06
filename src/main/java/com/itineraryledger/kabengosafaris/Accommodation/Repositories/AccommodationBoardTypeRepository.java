package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationBoardTypeRepository extends JpaRepository<AccommodationBoardType, Long>, JpaSpecificationExecutor<AccommodationBoardType> {

    /**
     * Find board type by accommodation ID and name
     */
    Optional<AccommodationBoardType> findByAccommodationIdAndName(Long accommodationId, String name);

    /**
     * Check if board type exists by accommodation ID and name
     */
    @Query("SELECT COUNT(b) > 0 FROM AccommodationBoardType b WHERE b.accommodation.id = :accommodationId AND b.name = :name")
    boolean existsByAccommodationIdAndName(@Param("accommodationId") Long accommodationId, @Param("name") String name);

    /**
     * Check if board type exists by accommodation ID and name, excluding specific ID
     */
    @Query("SELECT COUNT(b) > 0 FROM AccommodationBoardType b WHERE b.accommodation.id = :accommodationId AND b.name = :name AND b.id != :excludeId")
    boolean existsByAccommodationIdAndNameExcludingId(@Param("accommodationId") Long accommodationId, @Param("name") String name, @Param("excludeId") Long excludeId);
}
