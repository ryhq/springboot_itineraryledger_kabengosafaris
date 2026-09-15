package com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;

@Repository
public interface ItineraryInclusionRepository extends JpaRepository<ItineraryInclusion, Long> {

    List<ItineraryInclusion> findByItineraryIdOrderBySortOrderAscIdAsc(Long itineraryId);

    void deleteByItineraryId(Long itineraryId);

    boolean existsByItineraryId(Long itineraryId);

    long countByInclusionItemId(Long inclusionItemId);

    /** Which itineraries reference an item, for the "you cannot delete this yet" report. */
    @Query("SELECT DISTINCT i.itinerary.code FROM ItineraryInclusion i WHERE i.inclusionItem.id = :itemId")
    List<String> findItineraryCodesUsing(Long itemId);
}
