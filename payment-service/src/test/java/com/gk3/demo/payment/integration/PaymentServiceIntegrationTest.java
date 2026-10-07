package com.gk3.demo.payment.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.gk3.demo.events.EventType;
import com.gk3.demo.payment.domain.PaymentRepository;
import com.gk3.demo.payment.domain.PaymentStatus;
import com.gk3.demo.payment.inbox.InboxRepository;
import com.gk3.demo.payment.outbox.OutboxRepository;
import com.gk3.demo.payment.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Black-box integration test for payment-service: real Postgres (Testcontainers) + the real
 * embedded web server (Spring Boot on a random port) + a stubbed downstream orchestrator inbox
 * (WireMock), so the Outbox relay actually performs HTTP delivery and the Inbox endpoint actually
 * goes through Spring MVC/JPA - only the *other* microservice is replaced by a stub.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @RegisterExtension
    static final WireMockExtension ORCHESTRATOR_STUB = WireMockExtension.newInstance().build();

    @DynamicPropertySource
    static void orchestratorUrl(DynamicPropertyRegistry registry) {
        registry.add("app.orchestrator.url", () -> ORCHESTRATOR_STUB.baseUrl() + "/api/sagas/events");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private InboxRepository inboxRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void stubOrchestrator() {
        ORCHESTRATOR_STUB.stubFor(WireMock.post("/api/sagas/events").willReturn(WireMock.ok()));
    }

    @Test
    void creatingPaymentPersistsItAndDeliversPaymentCreatedThroughOutboxRelay() throws Exception {
        HttpResponse<String> response = post("/api/payments",
                """
                {"payerId":"alice","payeeId":"bob","amount":100,"currency":"EUR"}
                """);

        assertThat(response.statusCode()).isEqualTo(201);
        UUID paymentId = extractId(response.body());
        assertThat(paymentRepository.findById(paymentId)).isPresent();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var outboxMessage = outboxRepository.findAll().stream()
                    .filter(message -> message.getSagaId().equals(paymentId))
                    .findFirst()
                    .orElseThrow();
            assertThat(outboxMessage.getEventType()).isEqualTo(EventType.PAYMENT_CREATED);
            assertThat(outboxMessage.getStatus()).isEqualTo(OutboxStatus.SENT);
        });

        ORCHESTRATOR_STUB.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/api/sagas/events")));
    }

    @Test
    void confirmCommandFromInboxTransitionsPaymentToConfirmedAndIsIdempotent() throws Exception {
        HttpResponse<String> created = post("/api/payments",
                """
                {"payerId":"alice","payeeId":"bob","amount":100,"currency":"EUR"}
                """);
        UUID paymentId = extractId(created.body());
        UUID messageId = UUID.randomUUID();
        String confirmEnvelope = """
                {"messageId":"%s","type":"CONFIRM_PAYMENT_COMMAND","sourceService":"orchestrator-service",
                 "sagaId":"%s","occurredAt":"%s","payload":{"paymentId":"%s"}}
                """.formatted(messageId, paymentId, Instant.now(), paymentId);

        HttpResponse<String> first = post("/api/payments/confirmation", confirmEnvelope);
        HttpResponse<String> duplicate = post("/api/payments/confirmation", confirmEnvelope);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(duplicate.statusCode()).isEqualTo(200);
        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(inboxRepository.findAll().stream().filter(m -> m.getId().equals(messageId)).count()).isEqualTo(1);
    }

    @Test
    void cancelCommandFromInboxTransitionsPaymentToCancelledWithReason() throws Exception {
        HttpResponse<String> created = post("/api/payments",
                """
                {"payerId":"alice","payeeId":"SANCTIONED","amount":50,"currency":"EUR"}
                """);
        UUID paymentId = extractId(created.body());
        String cancelEnvelope = """
                {"messageId":"%s","type":"CANCEL_PAYMENT_COMMAND","sourceService":"orchestrator-service",
                 "sagaId":"%s","occurredAt":"%s","payload":{"paymentId":"%s","reason":"Matched sanctions list: SANCTIONED"}}
                """.formatted(UUID.randomUUID(), paymentId, Instant.now(), paymentId);

        post("/api/payments/confirmation", cancelEnvelope);

        var payment = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getCancelReason()).isEqualTo("Matched sanctions list: SANCTIONED");
    }

    private HttpResponse<String> post(String path, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static UUID extractId(String jsonBody) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"").matcher(jsonBody);
        if (!matcher.find()) {
            throw new IllegalStateException("No id field found in response body: " + jsonBody);
        }
        return UUID.fromString(matcher.group(1));
    }
}
