package com.dbtraining.reconx.observability;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * ============================================================================
 * TICKET-ADV059 — DatabaseHealthIndicator (timed SELECT 1)
 *
 * WHAT:    Custom actuator HealthIndicator that runs a fast `SELECT 1` with
 *          a 2-second timeout and reports elapsedMs as a detail.
 * HOW:     Extends AbstractHealthIndicator; Spring picks it up by bean name
 *          and exposes it under /actuator/health/reconxDatabase.
 * WHY:     The default DataSource health indicator works, but a custom one
 *          gives us a controllable timeout AND visible latency for SRE
 *          dashboards.
 * OBSERVE: GET /api/actuator/health/reconxDatabase -> `{"status":"UP",
 *          "details":{"query":"SELECT 1","elapsedMs": <number>}}`.
 *
 *  HINT: Throw any exception out of this method — AbstractHealthIndicator
 *        converts it to DOWN with the exception class as a detail.
 * ============================================================================
 */
@Component("reconxDatabase")
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private static final String QUERY = "SELECT 1";

    private final DataSource ds;

    public DatabaseHealthIndicator(DataSource ds) { this.ds = ds; }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        long start = System.nanoTime();
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.setQueryTimeout(2);
            s.execute(QUERY);
            builder.up()
                    .withDetail("query", QUERY)
                    .withDetail("elapsedMs", (System.nanoTime() - start) / 1_000_000);
        } catch (Exception ex) {
            builder.down(ex).withDetail("query", QUERY);
            throw ex;
        }
    }
}
