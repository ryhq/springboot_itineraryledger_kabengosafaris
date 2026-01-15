package com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItineraryPaxRepository extends JpaRepository<ItineraryPax, Long>, JpaSpecificationExecutor<ItineraryPax> {

    List<ItineraryPax> findByItineraryId(Long itineraryId);

    Optional<ItineraryPax> findByItineraryIdAndNationCategoryIdAndAgeCategoryId(
        Long itineraryId, Long nationCategoryId, Long ageCategoryId);

    boolean existsByItineraryIdAndNationCategoryIdAndAgeCategoryId(
        Long itineraryId, Long nationCategoryId, Long ageCategoryId);

    void deleteByItineraryId(Long itineraryId);

    long countByItineraryId(Long itineraryId);
}
