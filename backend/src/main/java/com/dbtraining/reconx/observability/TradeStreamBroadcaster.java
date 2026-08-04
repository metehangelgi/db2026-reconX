package com.dbtraining.reconx.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TICKET-ADV104 — Fan-out broadcaster for the /v1/trades/stream SSE endpoint.
 * Holds one SseEmitter per connected browser tab; broadcast() pushes to all
 * of them and prunes any that have disconnected/errored.
 */
@Component
public class TradeStreamBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(TradeStreamBroadcaster.class);
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout — client controls lifecycle
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        try {
            // Tomcat doesn't commit response headers until the first write, so
            // without this, EventSource.onopen never fires (and the client's
            // "connected" state never flips) until the first real trade event
            // happens to be broadcast — which could be minutes away or never.
            // An immediate comment forces the flush right at subscription time.
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void broadcast(Map<String, Object> trade) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(trade));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
                emitters.remove(emitter);
            } catch (IllegalStateException ex) {
                // A client that disconnected (closed tab, network drop, refresh)
                // can leave its AsyncContext already errored-out server-side —
                // sending to it then throws IllegalStateException, not IOException.
                // This must never escape: broadcast() runs inside the same
                // @Transactional call as the trade save, so an uncaught exception
                // here would roll back the trade write itself over one dead
                // SSE listener having nothing to do with persistence.
                log.debug("Dropping dead SSE emitter: {}", ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
