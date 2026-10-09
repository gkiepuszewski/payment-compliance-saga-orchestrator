package com.gk3.demo.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows browser-based clients (notably {@code payment-app}, the Flutter client, when run as a
 * web app via {@code flutter run -d chrome}) to call the Payment API directly from a different
 * origin. Server-to-server calls (orchestrator-service, curl, the e2e tests) never send an
 * {@code Origin} header and are therefore unaffected by CORS either way.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    public WebConfig(@Value("${app.cors.allowed-origins}") String[] allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/payments/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
