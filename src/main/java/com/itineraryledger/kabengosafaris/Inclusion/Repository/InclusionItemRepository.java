package com.itineraryledger.kabengosafaris.Inclusion.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;

@Repository
public interface InclusionItemRepository
        extends JpaRepository<InclusionItem, Long>, JpaSpecificationExecutor<InclusionItem> {

    /** What a new itinerary starts with, in print order. */
    List<InclusionItem> findByIsActiveTrueAndIsStandardTrueOrderByDisplayOrderAscIdAsc();

    List<InclusionItem> findByIsActiveTrueOrderByDisplayOrderAscIdAsc();

    /**
     * The seeder and the backfill both match on the wording, case- and space-insensitively.
     *
     * <p>A label is what a person typed; "Government taxes & levies" and "government taxes &
     * levies " are the same promise, and treating them as two would reintroduce the duplication
     * this whole change exists to remove.
     */
    @Query("SELECT i FROM InclusionItem i WHERE LOWER(TRIM(i.label)) = LOWER(TRIM(:label))")
    Optional<InclusionItem> findByLabelIgnoringCaseAndSpace(String label);

    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) FROM InclusionItem i")
    Integer findMaxDisplayOrder();
}
