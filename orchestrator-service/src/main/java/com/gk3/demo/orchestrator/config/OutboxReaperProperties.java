package com.gk3.demo.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.reaper")
public record OutboxReaperProperties(long fixedDelay) {
}
