package com.gk3.demo.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.relay")
public record OutboxRelayProperties(long fixedDelay, int batchSize, int maxAttempts) {
}
