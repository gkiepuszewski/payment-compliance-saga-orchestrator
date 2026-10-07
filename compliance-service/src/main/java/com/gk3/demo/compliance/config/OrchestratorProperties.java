package com.gk3.demo.compliance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.orchestrator")
public record OrchestratorProperties(String url) {
}
