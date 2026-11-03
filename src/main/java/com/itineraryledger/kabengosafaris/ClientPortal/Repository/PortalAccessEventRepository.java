package com.itineraryledger.kabengosafaris.ClientPortal.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    /**
     * How many of each thing happened on this journey, and when it last did.
     *
     * <p>Grouped in the database rather than by loading the rows. A busy journey holds hundreds
     * of these and the panel asks for this every time the tab is opened; there is also no reason
     * for the application to see a client's individual page views in order to count them.
     */
    @Query("select e.section, count(e), min(e.createdAt), max(e.createdAt)"
        + " from PortalAccessEvent e where e.journey.id = :journeyId group by e.section")
    List<Object[]> countBySection(@Param("journeyId") Long journeyId);

    /**
     * The same, per person.
     *
     * <p>Only the sections that say something about the customer are counted, so the figure
     * beside a name is what THEY did. Rows with no participant are either an unverified visitor
     * or us, and belong to neither.
     */
    @Query("select e.participant.id, count(e), min(e.createdAt), max(e.createdAt)"
        + " from PortalAccessEvent e where e.journey.id = :journeyId"
        + " and e.participant is not null and e.section in :sections"
        + " group by e.participant.id")
    List<Object[]> countByParticipant(@Param("journeyId") Long journeyId,
                                      @Param("sections") Collection<String> sections);

    /** When the customer was last doing something, as opposed to when we were. */
    @Query("select max(e.createdAt) from PortalAccessEvent e"
        + " where e.journey.id = :journeyId and e.section in :sections")
    LocalDateTime lastOf(@Param("journeyId") Long journeyId,
                         @Param("sections") Collection<String> sections);

    /** Whether anything in this set has happened since a moment. Used for "sent back and unread". */
    @Query("select count(e) from PortalAccessEvent e where e.journey.id = :journeyId"
        + " and e.section in :sections and e.createdAt > :after")
    long countSince(@Param("journeyId") Long journeyId,
                    @Param("sections") Collection<String> sections,
                    @Param("after") LocalDateTime after);

    /** What the retention job deletes. See PortalAccessEvent's note on why this one expires. */
    @Modifying
    @Query("delete from PortalAccessEvent e where e.journey.id = :journeyId")
    int deleteEveryEventOf(@Param("journeyId") Long journeyId);
}
