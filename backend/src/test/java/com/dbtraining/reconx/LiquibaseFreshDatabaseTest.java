package com.dbtraining.reconx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV079 — Verify Liquibase ran on a fresh DB.
 * WHAT:    Spins up a brand-new, empty Postgres container (Testcontainers) and lets
 *          Spring Boot / Liquibase migrate it from scratch on context startup, then
 *          asserts the changelog fully applied and seed data landed.
 * WHY:     This is the fastest possible signal that a developer forgot to commit a
 *          new changeset XML, broke the include order in db.changelog-master.xml, or
 *          introduced a changeset that fails against a truly empty schema (as opposed
 *          to an existing dev DB that already has the tables/data and would mask the
 *          problem). It intentionally asserts only row counts / existence, not business
 *          logic — that's covered elsewhere.
 * OBSERVE: On a fresh DB, Liquibase creates databasechangelog + databasechangeloglock,
 *          then applies every changeset from every <include> in db.changelog-master.xml
 *          (currently 001-init, 002-schema, 003-jsonb, 004-partitioning, 005-mat-views,
 *          006-audit-and-recon, 007-users-rbac, 008-seed, 011-mv-daily-recon-summary,
 *          012-recon-results, 013-envers, 014-resync-identity-sequences,
 *          015-recon-breaks-job-id — roughly 25 changesets as of writing). 008-seed.xml
 *          loads counterparties.csv (10 rows), instruments.csv (15 rows), and
 *          data/seed_data.sql (500 trades).
 */
@SpringBootTest
@Testcontainers
class LiquibaseFreshDatabaseTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Test
    void allChangesetsRanOnFreshDatabase() {
        // db.changelog-master.xml currently includes 13 files totaling ~25 changesets.
        // Use a conservative round-number floor rather than an exact count: the point
        // of this assertion is "the changelog actually ran end-to-end", not pinning a
        // brittle number that drifts every time someone adds a changeset.
        Integer ranChangesets = jdbc.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);

        assertThat(ranChangesets).isNotNull().isGreaterThanOrEqualTo(20);
    }

    @Test
    void seedDataLandedOnFreshDatabase() {
        // 008-seed.xml's sqlFile (data/seed_data.sql) inserts 500 trades on a fresh DB;
        // the ticket only asks us to prove seed data landed, so assert comfortably
        // below that (>= 10) rather than pinning the exact seeded row count.
        Integer nonDeletedTrades = jdbc.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE deleted_at IS NULL", Integer.class);

        assertThat(nonDeletedTrades).isNotNull().isGreaterThanOrEqualTo(10);
    }

    @Test
    void csvSeededReferenceDataLandedExactly() {
        // Sanity check that the *full* changelog applied, not just a partial subset:
        // counterparties.csv and instruments.csv are fixed baseline reference data.
        Integer counterparties = jdbc.queryForObject(
                "SELECT COUNT(*) FROM counterparties", Integer.class);
        Integer instruments = jdbc.queryForObject(
                "SELECT COUNT(*) FROM instruments", Integer.class);

        assertThat(counterparties).isEqualTo(10);
        assertThat(instruments).isEqualTo(15);
    }
}
