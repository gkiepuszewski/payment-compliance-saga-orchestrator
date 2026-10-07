package com.gk3.demo.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.orchestrator")
public record OrchestratorProperties(String url) {
}
