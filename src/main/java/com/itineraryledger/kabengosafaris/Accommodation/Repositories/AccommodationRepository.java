package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long>, JpaSpecificationExecutor<Accommodation> {

    /**
     * Find accommodation by exact name
     */
    Optional<Accommodation> findByName(String name);

    /**
     * Find accommodation by slug
     */
    Optional<Accommodation> findBySlug(String slug);

    /**
     * Check if accommodation exists by name
     */
    boolean existsByName(String name);

    /**
     * Check if accommodation exists by slug
     */
    boolean existsBySlug(String slug);
}
