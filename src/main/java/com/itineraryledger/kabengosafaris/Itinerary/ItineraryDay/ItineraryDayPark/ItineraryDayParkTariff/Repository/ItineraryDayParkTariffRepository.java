package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryDayParkTariffRepository extends JpaRepository<ItineraryDayParkTariff, Long>, JpaSpecificationExecutor<ItineraryDayParkTariff> {

    List<ItineraryDayParkTariff> findByItineraryDayParkId(Long itineraryDayParkId);

    void deleteByItineraryDayParkId(Long itineraryDayParkId);

    long countByItineraryDayParkId(Long itineraryDayParkId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within park visit)
    // ========================

    @Query("SELECT t.id FROM ItineraryDayParkTariff t WHERE t.itineraryDayPark.id = :parentId AND t.id > :currentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT t.id FROM ItineraryDayParkTariff t WHERE t.itineraryDayPark.id = :parentId AND t.id < :currentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT t.id FROM ItineraryDayParkTariff t WHERE t.itineraryDayPark.id = :parentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT t.id FROM ItineraryDayParkTariff t WHERE t.itineraryDayPark.id = :parentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
