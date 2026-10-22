package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalAccessEvent;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalDevice;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What the client opened, written down.
 *
 * <p>Two honest uses. Support: "she says the invoice is not there" is answerable in seconds if
 * you can see she opened the trip four times and the invoice never. And insight: where people
 * stall, and how long a quotation sits before anybody accepts it.
 *
 * <p>Recording never breaks the page. Its own transaction, every failure swallowed, exactly as
 * {@code AuditLogService} does it: a page that will not render because the analytics failed is a
 * worse outcome than analytics with a hole in them.
 *
 * <p>These rows have an expiry date, which almost nothing else in this schema does. They are
 * behaviour, attached to a named individual, and many of these people are in the EU, so
 * {@code PortalRetentionScheduler} destroys them three months after the trip ends or the booking
 * falls through. Nothing here is written that we would not be content to show the person it is
 * about.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalActivityService {

    private final PortalAccessEventRepository events;

    /**
     * @param section what was opened: TRIP, QUOTE, INVOICE, DOCUMENT, DEVICES, VERIFY_OK and so on
     * @param detail  which document or which day. Never free text typed by the client
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(PortalJourney journey, PortalParticipant participant, PortalDevice device,
                       String section, String detail, String ipAddress, String userAgent) {
        if (journey == null || section == null) return;
        try {
            events.save(PortalAccessEvent.builder()
                .journey(journey)
                .participant(participant)
                .device(device)
                .section(section)
                .detail(trim(detail, 200))
                .ipAddress(trim(ipAddress, 64))
                .userAgent(trim(userAgent, 500))
                .build());
        } catch (Exception e) {
            log.warn("Could not record portal activity {} on journey {}: {}",
                section, journey.getId(), e.getMessage());
        }
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
