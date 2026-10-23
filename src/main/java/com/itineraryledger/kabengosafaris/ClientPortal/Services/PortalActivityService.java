package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalAccessEvent;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalDevice;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What the client opened, written down.
 *
 * <p>Two honest uses. Support: "she says the invoice is not there" is answerable in seconds if
 * you can see she opened the trip four times and the invoice never. And insight: where people
 * stall, and how long a quotation sits before anybody accepts it.
 *
 * <p>Recording never breaks the page, and getting that right took two attempts. The first wrote
 * the row in a nested transaction, which deadlocked against the one that called it: signing in
 * creates a device and then records VERIFY_OK <em>pointing at that device</em>, so the nested
 * transaction's foreign-key check waited on a parent row the outer transaction had not committed
 * and was holding while it waited for the nested call to return. Fifty seconds of that, then a
 * lock-wait timeout, then {@code UnexpectedRollbackException} rolling back a sign-in that had
 * actually succeeded. The client saw "an unexpected error occurred" and could not read their
 * quotation.
 *
 * <p>So the row is written <b>after the caller commits</b>. The rows it refers to exist by then,
 * nothing it does can be rolled back into the caller's work, and an event is never written for
 * something that did not in the end happen. Outside a transaction it is written immediately,
 * which is what the read-only page views do.
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

    private final PortalActivityWriter writer;

    /**
     * @param section what was opened: TRIP, QUOTE, INVOICE, DOCUMENT, DEVICES, VERIFY_OK and so on
     * @param detail  which document or which day. Never free text typed by the client
     */
    public void record(PortalJourney journey, PortalParticipant participant, PortalDevice device,
                       String section, String detail, String ipAddress, String userAgent) {
        if (journey == null || section == null) return;

        PortalAccessEvent event = PortalAccessEvent.builder()
            .journey(journey)
            .participant(participant)
            .device(device)
            .section(section)
            .detail(trim(detail, 200))
            .ipAddress(trim(ipAddress, 64))
            .userAgent(trim(userAgent, 500))
            .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    writeQuietly(event, journey.getId(), section);
                }
            });
            return;
        }

        writeQuietly(event, journey.getId(), section);
    }

    /**
     * The catch has to be here, outside the proxy.
     *
     * <p>A {@code try/catch} inside a transactional method cannot see its own commit fail, which
     * is why the first version of this swallowed nothing it needed to.
     */
    private void writeQuietly(PortalAccessEvent event, Long journeyId, String section) {
        try {
            writer.write(event);
        } catch (Exception e) {
            log.warn("Could not record portal activity {} on journey {}: {}",
                section, journeyId, e.getMessage());
        }
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
