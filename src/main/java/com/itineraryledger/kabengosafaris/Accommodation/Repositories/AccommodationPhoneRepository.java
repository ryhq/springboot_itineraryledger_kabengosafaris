package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationPhoneRepository extends JpaRepository<AccommodationPhone, Long>, JpaSpecificationExecutor<AccommodationPhone> {

    /**
     * Find phone by exact phone number
     */
    Optional<AccommodationPhone> findByPhoneNumber(String phoneNumber);

    /**
     * Check if phone number exists
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Mark all phones for an accommodation as non-primary
     */
    @Modifying
    @Query("UPDATE AccommodationPhone p SET p.isPrimary = false WHERE p.accommodation.id = :accommodationId")
    void markAllAsNonPrimaryForAccommodation(@Param("accommodationId") Long accommodationId);

    /**
     * Mark all phones for an accommodation except one as non-primary
     */
    @Modifying
    @Query("UPDATE AccommodationPhone p SET p.isPrimary = false WHERE p.accommodation.id = :accommodationId AND p.id != :excludePhoneId")
    void markAllAsNonPrimaryExcept(@Param("accommodationId") Long accommodationId, @Param("excludePhoneId") Long excludePhoneId);
}
