package com.itineraryledger.kabengosafaris.Accommodation.Repositories;

import java.util.List;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long>, JpaSpecificationExecutor<Accommodation> {

    /**
     * Find accommodation by exact name
     */
    Optional<Accommodation> findByName(String name);

    /**
     * Everything live in one region, for the portal's catalogue.
     *
     * <p>The region is what keeps a swap deliverable. A client choosing from the catalogue is
     * changing where they sleep on a night whose route is already fixed: the vehicle arrives
     * from somewhere and leaves for somewhere, and a lodge four hours away is not an
     * alternative, it is a different trip.
     *
     * <p>Case-insensitive because the column is free text and has been typed by hand for years.
     */
    @Query("select a from Accommodation a where a.isActive = true "
        + "and lower(a.region) = lower(:region) order by a.name asc")
    List<Accommodation> findLiveInRegion(@Param("region") String region);

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

    // ========================
    // NAVIGATION QUERIES (circular next/previous)
    // ========================

    @Query("SELECT e.id FROM Accommodation e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Accommodation e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM Accommodation e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM Accommodation e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();

    /** Properties pointing at this vendor — checked before a vendor is deleted. */
    long countByVendorId(Long vendorId);

    /** The properties settled through one vendor account. One means we know which camp it was. */
    java.util.List<Accommodation> findByVendorId(Long vendorId);
}
