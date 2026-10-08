package com.gk3.demo.e2e;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Black-box end-to-end saga test: builds and runs the whole local stack (3 Spring Boot services +
 * 3 Postgres databases + Vault) through Testcontainers/docker-compose.e2e.yml, then drives the
 * saga purely through payment-service's public REST API and polls orchestrator-service's public
 * saga-query endpoint - exactly what the README's "Try it" section does manually, automated.
 *
 * <p>Opt-in only: requires Docker, builds 3 images and runs 7 containers, so it's slow. Run with:
 * {@code mvn verify -Pe2e -pl e2e-tests -am}
 */
@Testcontainers
class PaymentComplianceSagaE2ETest {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Container
    static final ComposeContainer COMPOSE = new ComposeContainer(new File("../docker-compose.e2e.yml"))
            .withExposedService("payment-service", 8081,
                    Wait.forLogMessage(".*Started PaymentServiceApplication.*\\n", 1)
                            .withStartupTimeout(Duration.ofMinutes(3)))
            .withExposedService("compliance-service", 8082,
                    Wait.forLogMessage(".*Started ComplianceServiceApplication.*\\n", 1)
                            .withStartupTimeout(Duration.ofMinutes(3)))
            .withExposedService("orchestrator-service", 8080,
                    Wait.forLogMessage(".*Started OrchestratorServiceApplication.*\\n", 1)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    @Test
    void happyPathPaymentIsConfirmedAndSagaCompletes() throws Exception {
        String body = """
                {"payerId":"ALICE","payeeId":"BOB","amount":150.00,"currency":"EUR"}
                """;

        String paymentId = extractId(post(paymentServiceUrl("/api/payments"), body));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(get(paymentServiceUrl("/api/payments/" + paymentId))).contains("\"status\":\"CONFIRMED\"");
            assertThat(get(orchestratorServiceUrl("/api/sagas/" + paymentId))).contains("\"state\":\"COMPLETED\"");
        });
    }

    @Test
    void compensationPathPaymentIsCancelledAndSagaCompensates() throws Exception {
        String body = """
                {"payerId":"SANCTIONED","payeeId":"BOB","amount":999.00,"currency":"USD"}
                """;

        String paymentId = extractId(post(paymentServiceUrl("/api/payments"), body));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(get(paymentServiceUrl("/api/payments/" + paymentId))).contains("\"status\":\"CANCELLED\"");
            assertThat(get(orchestratorServiceUrl("/api/sagas/" + paymentId))).contains("\"state\":\"COMPENSATED\"");
        });
    }

    private static String paymentServiceUrl(String path) {
        return "http://" + COMPOSE.getServiceHost("payment-service", 8081) + ":"
                + COMPOSE.getServicePort("payment-service", 8081) + path;
    }

    private static String orchestratorServiceUrl(String path) {
        return "http://" + COMPOSE.getServiceHost("orchestrator-service", 8080) + ":"
                + COMPOSE.getServicePort("orchestrator-service", 8080) + path;
    }

    private static String post(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("POST %s -> %s", url, response.body()).isEqualTo(201);
        return response.body();
    }

    private static String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("GET %s -> %s", url, response.body()).isEqualTo(200);
        return response.body();
    }

    private static String extractId(String jsonBody) {
        Matcher matcher = ID_PATTERN.matcher(jsonBody);
        if (!matcher.find()) {
            throw new IllegalStateException("No id field found in response body: " + jsonBody);
        }
        return matcher.group(1);
    }
}
