package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalAccessEvent;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;

/**
 * Writing down that somebody opened a page must never undo their opening it.
 *
 * <p>It did, on a live company, and the cause was not visible in the code that failed. Signing
 * in creates a device row and then records VERIFY_OK pointing at it. Recording ran in a nested
 * transaction, so its foreign-key check waited on a parent row the outer transaction was still
 * holding — while that outer transaction waited for the nested call to return. Fifty seconds
 * later: lock-wait timeout, then {@code UnexpectedRollbackException}, and a client who had
 * typed the right code was told "an unexpected error occurred" and shown no quotation.
 *
 * <p>Two properties keep it fixed, and the first one is what makes the deadlock impossible:
 * inside a transaction nothing is written until that transaction has committed, so the rows
 * being pointed at already exist. The second is the belt: a write that fails anyway is
 * swallowed.
 *
 * <p>In this package rather than beside the other portal tests, because the writer it has to
 * stand in for is package-private — which is itself deliberate, since the whole point is that
 * nothing outside this package may write one of these rows inside somebody else's transaction.
 */
class RecordingAVisitCannotUndoTheVisitTest {

    @AfterEach
    void clearAnySynchronisation() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private PortalJourney journey() {
        PortalJourney journey = PortalJourney.builder().build();
        journey.setId(7L);
        return journey;
    }

    @Test
    @DisplayName("inside a transaction, nothing is written until that transaction commits")
    void deferredUntilAfterCommit() {
        PortalActivityWriter writer = mock(PortalActivityWriter.class);
        PortalActivityService service = new PortalActivityService(writer);

        TransactionSynchronizationManager.initSynchronization();
        service.record(journey(), null, null, "VERIFY_OK", null, "1.2.3.4", "agent");

        verify(writer, never()).write(any(PortalAccessEvent.class));

        List<TransactionSynchronization> registered =
            TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, registered.size(), "the write must be queued, not performed");

        registered.get(0).afterCommit();
        verify(writer, times(1)).write(any(PortalAccessEvent.class));
    }

    @Test
    @DisplayName("outside a transaction it is written at once, which is the page-view case")
    void immediateWithoutATransaction() {
        PortalActivityWriter writer = mock(PortalActivityWriter.class);
        PortalActivityService service = new PortalActivityService(writer);

        service.record(journey(), null, null, "TRIP", null, "1.2.3.4", "agent");

        verify(writer, times(1)).write(any(PortalAccessEvent.class));
    }

    @Test
    @DisplayName("a write that fails is swallowed, because analytics do not get a vote")
    void failureIsSwallowed() {
        PortalActivityWriter writer = mock(PortalActivityWriter.class);
        doThrow(new IllegalStateException("lock wait timeout"))
            .when(writer).write(any(PortalAccessEvent.class));
        PortalActivityService service = new PortalActivityService(writer);

        assertDoesNotThrow(() -> service.record(journey(), null, null, "TRIP", null, null, null),
            "a page that will not load because the analytics failed is the worse outcome");
    }

    @Test
    @DisplayName("a journey or a section we cannot name is not written at all")
    void nothingToSay() {
        PortalActivityWriter writer = mock(PortalActivityWriter.class);
        PortalActivityService service = new PortalActivityService(writer);

        service.record(null, null, null, "TRIP", null, null, null);
        service.record(journey(), null, null, null, null, null, null);

        verify(writer, never()).write(any(PortalAccessEvent.class));
    }
}
