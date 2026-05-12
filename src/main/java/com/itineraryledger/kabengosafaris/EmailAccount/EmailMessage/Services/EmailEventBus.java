package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * §9 — in-process SSE event bus. Each subscriber (by accountId) gets a
 * dedicated emitter; publishers fan out to every emitter for the matching
 * account.
 *
 * This is deliberately minimal — single-instance only. If the app ever
 * runs multi-replica, swap this for Redis Pub/Sub or similar.
 */
@Service
@Slf4j
public class EmailEventBus {

    private final Map<Long, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long accountId) {
        SseEmitter emitter = new SseEmitter(60L * 60L * 1000L); // 1h
        subscribers.computeIfAbsent(accountId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(accountId, emitter));
        emitter.onTimeout(() -> remove(accountId, emitter));
        emitter.onError(e -> remove(accountId, emitter));
        try {
            // Send an initial hello so the connection is flushed.
            emitter.send(SseEmitter.event().name("ready").data(Map.of("accountId", accountId)));
        } catch (IOException e) {
            remove(accountId, emitter);
        }
        return emitter;
    }

    private void remove(Long accountId, SseEmitter emitter) {
        List<SseEmitter> list = subscribers.get(accountId);
        if (list != null) list.remove(emitter);
    }

    /** Fan out an event to every emitter on this account. */
    public void publish(Long accountId, String eventName, Object payload) {
        List<SseEmitter> list = subscribers.get(accountId);
        if (list == null || list.isEmpty()) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                log.debug("SSE send failed for account {}: {}", accountId, e.getMessage());
                remove(accountId, emitter);
            }
        }
    }
}
