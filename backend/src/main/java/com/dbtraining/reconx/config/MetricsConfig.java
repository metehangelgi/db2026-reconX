package com.dbtraining.reconx.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * TICKET-ADV084 — enables {@code @Timed}
 *
 * WHY: Spring Boot's actuator autoconfiguration does NOT register a
 *      TimedAspect automatically — without this bean, every {@code @Timed}
 *      annotation in the codebase (e.g. ReconciliationEngine.reconcile) is
 *      inert and silently produces no metric at all.
 * ============================================================================
 */
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
