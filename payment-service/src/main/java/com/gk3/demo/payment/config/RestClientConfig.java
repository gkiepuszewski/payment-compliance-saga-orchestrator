package com.gk3.demo.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        // Force HTTP/1.1: the JDK HttpClient's HTTP/2 upgrade negotiation is not reliably
        // compatible with every downstream server implementation (e.g. WireMock's embedded
        // Jetty in tests), and plain REST calls between these services gain nothing from HTTP/2.
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(jdkHttpClient))
                .build();
    }
}
