package com.dbtraining.reconx.observability;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Regression test for the bug where a dead SSE client (closed tab, dropped
 * connection) made {@code broadcast()} throw IllegalStateException instead of
 * IOException — which escaped uncaught and rolled back the trade-creation
 * transaction it was called from. See TradeService.create().
 */
class TradeStreamBroadcasterTest {

    private final TradeStreamBroadcaster broadcaster = new TradeStreamBroadcaster();

    @Test
    void broadcast_emitterThrowsIOException_isPrunedAndDoesNotPropagate() throws IOException {
        SseEmitter deadEmitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(deadEmitter).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));
        setEmitters(deadEmitter);

        assertThatCode(() -> broadcaster.broadcast(Map.of("tradeRef", "T-1")))
                .doesNotThrowAnyException();

        verify(deadEmitter, times(1)).completeWithError(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void broadcast_emitterThrowsIllegalStateException_isPrunedAndDoesNotPropagate() throws IOException {
        SseEmitter deadEmitter = mock(SseEmitter.class);
        doThrow(new IllegalStateException(
                "A non-container (application) thread attempted to use the AsyncContext after an error had occurred"))
                .when(deadEmitter).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));
        setEmitters(deadEmitter);

        assertThatCode(() -> broadcaster.broadcast(Map.of("tradeRef", "T-2")))
                .doesNotThrowAnyException();
    }

    private void setEmitters(SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
        emitters.add(emitter);
        ReflectionTestUtils.setField(broadcaster, "emitters", emitters);
    }
}
