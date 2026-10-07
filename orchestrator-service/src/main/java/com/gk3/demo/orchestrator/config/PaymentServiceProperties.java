package com.gk3.demo.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment-service")
public record PaymentServiceProperties(String url) {
}
