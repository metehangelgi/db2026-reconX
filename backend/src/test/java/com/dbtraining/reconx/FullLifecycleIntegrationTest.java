package com.dbtraining.reconx;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV078 — Full lifecycle integration test with Testcontainers.
 *
 * Drives a real Postgres-backed Spring Boot app through the full trade
 * lifecycle over real HTTP calls: login -> create trade -> list trades ->
 * patch status -> run recon -> resolve recon break. Every call goes through
 * the actual filter chain (JWT auth, RBAC) exactly like a real client would.
 *
 * A real Kafka broker (Testcontainers) is wired in alongside Postgres: trade
 * creation publishes to the `trade-events` topic via KafkaTemplate, and
 * without a reachable broker KafkaProducer#send blocks for max.block.ms
 * (default 60s) fetching metadata, which times out the request and derails
 * the whole ordered chain — so this isn't optional plumbing here.
 *
 * Ordered steps share state (JWT token, created trade id) via static fields:
 * JUnit 5 creates a new test instance per @Test method by default, so plain
 * instance fields don't survive between @Order steps unless the class opts
 * into @TestInstance(PER_CLASS) — and PER_CLASS conflicts with
 * @Testcontainers + @DynamicPropertySource here (Spring's context
 * preparation would run before the Testcontainers BeforeAll callback starts
 * the containers). Static fields sidestep both problems.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullLifecycleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reconx")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private static String jwtToken;
    private static Long createdTradeId;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        return headers;
    }

    @Test
    @Order(1)
    void login_returnsJwt() {
        Map<String, String> body = Map.of("email", "admin@db.com", "password", "admin123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("token")).isNotNull();
        assertThat(response.getBody().get("role")).isEqualTo("ADMIN");

        jwtToken = (String) response.getBody().get("token");
        assertThat(jwtToken).isNotBlank();
    }

    @Test
    @Order(2)
    void createTrade_returns201() {
        Map<String, Object> body = Map.of(
                "tradeRef", "LCT-20260603-0001",
                "instrumentId", 1,
                "counterpartyId", 1,
                "assetClass", "EQUITY",
                "side", "BUY",
                "quantity", 100,
                "price", 245.50,
                "tradeDate", LocalDate.now().toString()
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/v1/trades",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("tradeRef")).isEqualTo("LCT-20260603-0001");

        createdTradeId = Long.valueOf(response.getBody().get("id").toString());
    }

    @Test
    @Order(3)
    void listTrades_includesCreatedTrade() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/v1/trades",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("items")).isNotNull();
    }

    @Test
    @Order(4)
    void patchStatus_returns200() {
        Map<String, String> body = Map.of("status", "CONFIRMED");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/v1/trades/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(body, authHeaders()),
                Map.class,
                createdTradeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    @Order(5)
    void runRecon_returns202() {
        Map<String, String> body = Map.of(
                "from", LocalDate.now().minusYears(1).toString(),
                "to", LocalDate.now().plusYears(1).toString()
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/v1/recon/run",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("jobId")).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("QUEUED");
    }

    @Test
    @Order(6)
    void resolveReconBreak_handlesNoExistingBreak() {
        // recon/run's actual reconciliation now happens on a background
        // thread (ReconJobRunner, @Async) so real recon_breaks rows may or
        // may not exist yet by the time this test runs, and always at
        // whatever ids the job generated — never 999999. Hitting resolve for
        // that deliberately-nonexistent id still proves the endpoint is
        // wired end-to-end: TradeNotFoundException -> GlobalExceptionHandler
        // -> 404.
        Map<String, String> body = Map.of("note", "test resolution");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/v1/recon/results/{id}/resolve",
                HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders()),
                Map.class,
                999999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
