package com.itineraryledger.kabengosafaris.ParkTariff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkTariff join entity
 *
 * Manages park-tariff associations with standard CRUD operations
 * plus custom queries for specific business needs.
 */
@Repository
public interface ParkTariffRepository extends JpaRepository<ParkTariff, ParkTariffId> {

    /**
     * Find all park-tariff associations for a specific park
     */
    List<ParkTariff> findByParkId(Long parkId);

    /**
     * Find all park-tariff associations for a specific tariff
     */
    List<ParkTariff> findByTariffId(Long tariffId);

    /**
     * Check if a specific park-tariff association exists
     */
    boolean existsByParkIdAndTariffId(Long parkId, Long tariffId);

    /**
     * Find a specific park-tariff association
     */
    Optional<ParkTariff> findByParkIdAndTariffId(Long parkId, Long tariffId);

    /**
     * Delete all associations for a specific park
     */
    void deleteByParkId(Long parkId);

    /**
     * Delete all associations for a specific tariff
     */
    void deleteByTariffId(Long tariffId);

    /**
     * Delete a specific park-tariff association
     */
    void deleteByParkIdAndTariffId(Long parkId, Long tariffId);

    /**
     * Count tariffs for a specific park
     */
    long countByParkId(Long parkId);

    /**
     * Count parks for a specific tariff
     */
    long countByTariffId(Long tariffId);

    /**
     * Get all tariffs for a park (eager fetch)
     */
    @Query("SELECT pt FROM ParkTariff pt JOIN FETCH pt.tariff WHERE pt.park.id = :parkId")
    List<ParkTariff> findByParkIdWithTariff(@Param("parkId") Long parkId);

    /**
     * Get all parks for a tariff (eager fetch)
     */
    @Query("SELECT pt FROM ParkTariff pt JOIN FETCH pt.park WHERE pt.tariff.id = :tariffId")
    List<ParkTariff> findByTariffIdWithPark(@Param("tariffId") Long tariffId);

    /**
     * Get all active tariffs for a park (eager fetch)
     */
    @Query("SELECT pt FROM ParkTariff pt JOIN FETCH pt.tariff t WHERE pt.park.id = :parkId AND t.isActive = true")
    List<ParkTariff> findActiveTariffsByParkId(@Param("parkId") Long parkId);
}
