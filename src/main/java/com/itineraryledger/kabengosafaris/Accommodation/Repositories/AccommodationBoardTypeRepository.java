package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Find unique board types based on meal configuration (grouped by meal settings)
     * Returns one board type per unique meal configuration, sorted by name
     */
    @Query("""
        SELECT b FROM AccommodationBoardType b
        WHERE b.id IN (
            SELECT MIN(b2.id)
            FROM AccommodationBoardType b2
            WHERE b2.isActive = true
            GROUP BY b2.mealsIncluded,
                     b2.breakfastIncluded,
                     b2.lunchIncluded,
                     b2.dinnerIncluded,
                     b2.snacksIncluded,
                     b2.drinksIncluded,
                     b2.alcoholicDrinksIncluded
        )
        ORDER BY b.name ASC
        """)
    List<AccommodationBoardType> findUniqueBoardTypesByMealConfiguration();
}
