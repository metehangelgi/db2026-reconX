package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.observability.TradeStreamBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * TICKET-ADV104 — GET /v1/trades/stream: SSE subscription for the live trade feed.
 */
@RestController
@RequestMapping("/v1/trades")
@Tag(name = "trades")
public class TradeStreamController {

    private final TradeStreamBroadcaster broadcaster;

    public TradeStreamController(TradeStreamBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(path = "/stream", produces = "text/event-stream")
    @Operation(summary = "Live SSE feed of trade create/update events")
    public SseEmitter stream() {
        return broadcaster.subscribe();
    }
}
