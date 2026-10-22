package com.itineraryledger.kabengosafaris.ClientPortal.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;

@Repository
public interface PortalParticipantRepository extends JpaRepository<PortalParticipant, Long> {

    /** Email is stored lowercase; callers normalise before asking. */
    Optional<PortalParticipant> findByJourneyIdAndEmail(Long journeyId, String email);

    List<PortalParticipant> findByJourneyIdOrderByIdAsc(Long journeyId);
}
