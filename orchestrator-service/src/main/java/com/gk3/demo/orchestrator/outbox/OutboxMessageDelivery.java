package com.gk3.demo.orchestrator.outbox;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.events.EventEnvelope;
import com.gk3.demo.orchestrator.config.OutboxRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Delivers a single outbox message over REST, each in its own {@code REQUIRES_NEW} transaction.
 * Kept as a separate bean (rather than a method on {@link OutboxRelay}) because Spring's
 * {@code @Transactional} is proxy-based: a self-invocation (e.g. {@code this.deliver(message)}
 * from within {@code OutboxRelay} itself) bypasses the proxy entirely, silently making the
 * annotation a no-op. Going through this separate, Spring-managed bean ensures the proxy -
 * and therefore the per-message transaction boundary - is actually applied.
 */
@Component
public class OutboxMessageDelivery {

    private static final Logger log = LoggerFactory.getLogger(OutboxMessageDelivery.class);

    private final OutboxRepository outboxRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OutboxRelayProperties properties;

    public OutboxMessageDelivery(OutboxRepository outboxRepository, RestClient restClient, ObjectMapper objectMapper,
                                  OutboxRelayProperties properties) {
        this.outboxRepository = outboxRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(OutboxMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayloadJson(), Map.class);
            EventEnvelope envelope = new EventEnvelope(
                    message.getId(),
                    message.getEventType(),
                    "orchestrator-service",
                    message.getSagaId(),
                    Instant.now(),
                    payload
            );
            restClient.post()
                    .uri(message.getTargetUrl())
                    .body(envelope)
                    .retrieve()
                    .toBodilessEntity();
            message.markSent();
            outboxRepository.save(message);
            log.info("Delivered outbox message {} ({}) to {}", message.getId(), message.getEventType(), message.getTargetUrl());
        } catch (Exception ex) {
            message.markFailedAttempt(ex.getMessage(), properties.maxAttempts());
            outboxRepository.save(message);
            log.warn("Failed to deliver outbox message {} ({}), attempt {}: {}",
                    message.getId(), message.getEventType(), message.getAttempts(), ex.getMessage());
        }
    }
}
