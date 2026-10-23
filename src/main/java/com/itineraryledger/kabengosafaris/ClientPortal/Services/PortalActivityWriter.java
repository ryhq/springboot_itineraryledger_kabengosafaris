package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalAccessEvent;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * The one statement that writes an access event, in a transaction of its own.
 *
 * <p>A separate bean on purpose. {@code @Transactional} is applied by a proxy, so a method
 * calling an annotated method on {@code this} gets no transaction at all — and, worse for the
 * bug this exists to fix, a {@code try/catch} written <em>inside</em> an annotated method never
 * sees the failure, because the commit that fails happens after the method has returned. The
 * catch has to live outside the proxy, which means the write has to live behind one.
 */
@Service
@RequiredArgsConstructor
class PortalActivityWriter {

    private final PortalAccessEventRepository events;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void write(PortalAccessEvent event) {
        events.save(event);
    }
}
