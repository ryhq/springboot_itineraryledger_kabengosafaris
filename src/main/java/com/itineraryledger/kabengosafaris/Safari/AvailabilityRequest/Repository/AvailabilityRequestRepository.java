package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest;

public interface AvailabilityRequestRepository
    extends JpaRepository<AvailabilityRequest, Long>, JpaSpecificationExecutor<AvailabilityRequest> {

    List<AvailabilityRequest> findBySafariIdOrderBySentAtDesc(Long safariId);

    /** Every request touching a safari's stays, for the one-call coverage read. */
    @Query("""
        select distinct r from AvailabilityRequest r
        left join fetch r.stays s
        where r.safari.id = :safariId
        order by r.sentAt desc
        """)
    List<AvailabilityRequest> findWithStaysBySafari(@Param("safariId") Long safariId);

    /** What a reply names in its In-Reply-To / References header. */
    Optional<AvailabilityRequest> findFirstByRfcMessageId(String rfcMessageId);

    List<AvailabilityRequest> findByThreadId(String threadId);

    /** Still open and asked about a particular stay — the guard's question. */
    @Query("""
        select r from AvailabilityRequest r
        join r.stays s
        where s.stay.id = :stayId and r.status <> com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest$Status.CLOSED
        order by r.sentAt desc
        """)
    List<AvailabilityRequest> findOpenForStay(@Param("stayId") Long stayId);

    /**
     * Open requests for one property — the last resort when a reply arrives with no usable headers.
     *
     * A lodge answering from a different address than the one written to, through a mail client that
     * drops In-Reply-To, is not an edge case; it is Tuesday.
     */
    @Query("""
        select r from AvailabilityRequest r
        where r.accommodation.id = :accommodationId
          and r.status = com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest$Status.SENT
        order by r.sentAt desc
        """)
    List<AvailabilityRequest> findAwaitingReplyForAccommodation(@Param("accommodationId") Long accommodationId);

    /**
     * Requests that named a sent message but never recorded its Message-ID.
     *
     * Every request written before the send path passed the header back is in this state, which is
     * why nothing could ever be matched automatically: the matcher read `rfcMessageId` and the
     * create path only ever wrote `emailMessageId`. They are repaired from the sent message itself.
     */
    @Query("""
        select r from AvailabilityRequest r
        where r.status <> com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest$Status.CLOSED
          and r.emailMessageId is not null
          and (r.rfcMessageId is null or r.subject is null or r.toAddress is null)
        """)
    List<AvailabilityRequest> findNeedingMailRepair();

    /**
     * Everything still waiting on an answer, newest first — the pool a stray reply is matched
     * against when its headers cannot say which ask it belongs to.
     */
    @Query("""
        select r from AvailabilityRequest r
        where r.status = com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest$Status.SENT
        order by r.sentAt desc
        """)
    List<AvailabilityRequest> findChaseDueOrAwaiting();

    /** The morning list: asked, no reply, and past its chase date. */
    @Query("""
        select r from AvailabilityRequest r
        where r.status = com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest$Status.SENT
          and r.chaseDueAt is not null and r.chaseDueAt <= :now
        order by r.chaseDueAt asc
        """)
    List<AvailabilityRequest> findChaseDue(@Param("now") LocalDateTime now);
}
