package com.dbtraining.reconx.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TICKET-ADV060 — Custom actuator HealthIndicator for Kafka connectivity.
 * Exposed under /actuator/health/kafka. Reports UP with broker cluster id and
 * node count on success; AbstractHealthIndicator converts any thrown exception
 * to DOWN automatically.
 */
@Component("kafka")
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final String bootstrapServers;

    public KafkaHealthIndicator(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            String clusterId = admin.describeCluster().clusterId().get(2, TimeUnit.SECONDS);
            int nodeCount = admin.describeCluster().nodes().get(2, TimeUnit.SECONDS).size();
            builder.up().withDetail("clusterId", clusterId).withDetail("nodes", nodeCount);
        }
    }
}
