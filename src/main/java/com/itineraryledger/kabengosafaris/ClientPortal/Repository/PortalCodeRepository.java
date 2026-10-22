package com.itineraryledger.kabengosafaris.ClientPortal.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalCode;

@Repository
public interface PortalCodeRepository extends JpaRepository<PortalCode, Long> {

    /** The newest unspent code for this person; older ones are dead the moment a new one is sent. */
    Optional<PortalCode> findFirstByParticipantIdAndUsedAtIsNullOrderByCreatedAtDesc(Long participantId);

    List<PortalCode> findByParticipantId(Long participantId);

    @Modifying
    @Query("update PortalCode c set c.usedAt = :at where c.participant.id = :participantId"
        + " and c.usedAt is null")
    int spendEveryOutstandingCode(@Param("participantId") Long participantId,
                                  @Param("at") LocalDateTime at);

    @Modifying
    @Query("delete from PortalCode c where c.participant.journey.id = :journeyId")
    int deleteEveryCodeOf(@Param("journeyId") Long journeyId);

    /** Housekeeping: an expired code is of no use to anybody, including us. */
    @Modifying
    @Query("delete from PortalCode c where c.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") LocalDateTime before);
}
