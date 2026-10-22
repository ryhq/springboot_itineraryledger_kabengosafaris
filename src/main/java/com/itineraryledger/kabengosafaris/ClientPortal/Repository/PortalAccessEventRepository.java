package com.itineraryledger.kabengosafaris.ClientPortal.Repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalAccessEvent;

@Repository
public interface PortalAccessEventRepository extends JpaRepository<PortalAccessEvent, Long> {

    List<PortalAccessEvent> findByJourneyIdOrderByCreatedAtDesc(Long journeyId, Pageable pageable);

    long countByJourneyId(Long journeyId);

    /** What the retention job deletes. See PortalAccessEvent's note on why this one expires. */
    @Modifying
    @Query("delete from PortalAccessEvent e where e.journey.id = :journeyId")
    int deleteEveryEventOf(@Param("journeyId") Long journeyId);
}
