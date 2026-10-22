package com.itineraryledger.kabengosafaris.ClientPortal.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;

@Repository
public interface PortalJourneyRepository extends JpaRepository<PortalJourney, Long> {

    /** The only way a page ever resolves a journey from the outside world. */
    Optional<PortalJourney> findByToken(String token);

    Optional<PortalJourney> findByQuoteId(Long quoteId);

    List<PortalJourney> findByCustomerId(Long customerId);

    boolean existsByCustomerIdAndItineraryId(Long customerId, Long itineraryId);

    Optional<PortalJourney> findByCustomerIdAndItineraryId(Long customerId, Long itineraryId);

    /**
     * Journeys whose behavioural data is now older than we said we would keep it.
     *
     * <p>Driven by the stored date rather than by re-deriving it from the safari, so a trip whose
     * dates move cannot silently extend how long somebody's browsing history is held.
     */
    @Query("select j from PortalJourney j where j.purgeAfter is not null and j.purgeAfter <= :on")
    List<PortalJourney> dueForPurge(@Param("on") LocalDate on);

    /**
     * Journeys whose trip is over or whose booking fell through, and which nobody has closed.
     *
     * <p>Without this the retention promise never fires. Closing is what writes the deletion
     * date, and nothing in the ordinary run of a booking has a reason to think about a portal
     * link, so the sweep looks for the two ends rather than asking eight call sites to remember.
     *
     * <p>A safari that has finished, or, where there is no safari, a quotation that was rejected,
     * cancelled or allowed to expire. An ACCEPTED quote with no safari yet is deliberately not
     * here: that is a live booking in the middle of being set up.
     */
    @Query("""
        select j from PortalJourney j
        where j.status = com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus.OPEN
          and (
            (j.safari is not null and j.safari.endDate is not null and j.safari.endDate < :on)
            or (j.safari is null and j.quote is not null and j.quote.status in
                 (com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus.REJECTED,
                  com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus.CANCELLED,
                  com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus.EXPIRED))
          )
        """)
    List<PortalJourney> dueToClose(@Param("on") LocalDate on);
}
