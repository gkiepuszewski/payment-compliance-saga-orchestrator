package com.gk3.demo.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.compliance-service")
public record ComplianceServiceProperties(String inboxUrl) {
}
