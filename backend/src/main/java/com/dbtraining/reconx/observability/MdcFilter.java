package com.dbtraining.reconx.observability;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * ============================================================================
 * TICKET-ADV061 — MdcFilter (structured logging correlation id)
 *
 * WHAT:    Reads `X-Correlation-Id` (generating a UUID if absent) and the
 *          optional `X-Trade-Ref` request headers, and puts both into SLF4J's
 *          MDC for the lifetime of the request.
 * HOW:     Implements Filter directly (not OncePerRequestFilter) and runs at
 *          @Order(1) so MDC is populated before any other filter/interceptor
 *          logs. Paired with `logback-spring.xml`, which has a `dev` profile
 *          plain pattern including `%X{correlationId:-}` / `%X{tradeRef:-}`
 *          and a `uat,prod` profile using LogstashEncoder with includeMdc.
 * WHY:     Day 6's Kafka consumer, Day 8's recon engine, and Day 10's Docker
 *          Compose stack all touch the same request/trade — without a
 *          correlation id stitched through MDC, tracing one trade across
 *          services becomes log archaeology.
 * OBSERVE: `curl -H "X-Correlation-Id: foo-123" http://localhost:8080/api/v1/trades`
 *          under `dev` writes a log line containing `foo-123`; under `uat` the
 *          same request emits a JSON line with `"correlationId":"foo-123"`.
 * ============================================================================
 *
 *  TODO(TICKET-ADV061):
 *    String correlationId = header(http, HDR_CORRELATION, UUID.randomUUID().toString());
 *    String tradeRef      = header(http, HDR_TRADE_REF, null);
 *    try {
 *        MDC.put("correlationId", correlationId);
 *        if (tradeRef != null) MDC.put("tradeRef", tradeRef);
 *        chain.doFilter(req, res);
 *    } finally {
 *        MDC.clear();
 *    }
 *
 *  HINT: The `finally` clear is what stops the id leaking into the next
 *        request handled by the same pooled Tomcat thread. MDC does not
 *        survive across `@Async` without extra help — that's a Day 7 concern.
 * ============================================================================
 */
@Component
@Order(1)
public class MdcFilter implements Filter {

    static final String HDR_CORRELATION = "X-Correlation-Id";
    static final String HDR_TRADE_REF   = "X-Trade-Ref";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        String correlationId = header(http, HDR_CORRELATION, UUID.randomUUID().toString());
        String tradeRef      = header(http, HDR_TRADE_REF, null);
        if (res instanceof HttpServletResponse httpRes) {
            httpRes.setHeader(HDR_CORRELATION, correlationId);
        }
        try {
            MDC.put("correlationId", correlationId);
            if (tradeRef != null) MDC.put("tradeRef", tradeRef);
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }

    private static String header(HttpServletRequest r, String name, String fallback) {
        String v = r.getHeader(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
