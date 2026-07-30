package com.dbtraining.reconx.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================================
 * TICKET-ADV082 — Per-cache TTLs: instruments 5 min, counterparties 1 min
 *
 * WHY: application.yml's single `spring.cache.caffeine.spec` applies the same
 *      TTL to every cache name. Instrument reference data changes rarely and
 *      can sit for 5 minutes; counterparty data (credit limits, status) is
 *      more volatile and needs a much shorter TTL.
 * ============================================================================
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache instruments = new CaffeineCache("instruments",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        CaffeineCache counterparties = new CaffeineCache("counterparties",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(instruments, counterparties));
        return manager;
    }
}
