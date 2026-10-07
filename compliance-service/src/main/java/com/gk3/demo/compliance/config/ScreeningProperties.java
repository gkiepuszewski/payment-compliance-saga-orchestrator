package com.gk3.demo.compliance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "compliance")
public record ScreeningProperties(List<String> sanctionedParties) {
}
