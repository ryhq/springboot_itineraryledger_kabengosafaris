package com.itineraryledger.kabengosafaris.ClientPortal.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalDevice;

@Repository
public interface PortalDeviceRepository extends JpaRepository<PortalDevice, Long> {

    /** The session lookup, once per request. Hashed, so a stolen table is not a stolen session. */
    Optional<PortalDevice> findByTokenHash(String tokenHash);

    List<PortalDevice> findByParticipantIdOrderByLastSeenAtDesc(Long participantId);

    @Query("select d from PortalDevice d where d.participant.journey.id = :journeyId"
        + " order by d.lastSeenAt desc")
    List<PortalDevice> findByJourneyId(@Param("journeyId") Long journeyId);

    @Modifying
    @Query("update PortalDevice d set d.revokedAt = :at where d.participant.journey.id = :journeyId"
        + " and d.revokedAt is null")
    int revokeEveryDeviceOf(@Param("journeyId") Long journeyId, @Param("at") LocalDateTime at);

    /**
     * End every live session belonging to one person, leaving the rest of the booking alone.
     *
     * <p>What makes taking a reader off the booking mean anything. Marking the participant
     * inactive stops them verifying again; it does nothing at all to the browser they are
     * already signed in on, which stays good for weeks.
     */
    @Modifying
    @Query("update PortalDevice d set d.revokedAt = :at where d.participant.id = :participantId"
        + " and d.revokedAt is null")
    int revokeEveryDeviceOfParticipant(@Param("participantId") Long participantId,
                                       @Param("at") LocalDateTime at);

    @Modifying
    @Query("delete from PortalDevice d where d.participant.journey.id = :journeyId")
    int deleteEveryDeviceOf(@Param("journeyId") Long journeyId);
}
