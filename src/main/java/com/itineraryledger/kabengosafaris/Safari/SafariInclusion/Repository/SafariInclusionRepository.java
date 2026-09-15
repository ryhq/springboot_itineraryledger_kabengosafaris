package com.itineraryledger.kabengosafaris.Safari.SafariInclusion.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariInclusion.Entity.SafariInclusion;

@Repository
public interface SafariInclusionRepository extends JpaRepository<SafariInclusion, Long> {

    List<SafariInclusion> findBySafariIdOrderBySortOrderAscIdAsc(Long safariId);

    void deleteBySafariId(Long safariId);

    boolean existsBySafariId(Long safariId);
}
