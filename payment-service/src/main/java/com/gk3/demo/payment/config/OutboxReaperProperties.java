package com.gk3.demo.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.reaper")
public record OutboxReaperProperties(long fixedDelay) {
}
