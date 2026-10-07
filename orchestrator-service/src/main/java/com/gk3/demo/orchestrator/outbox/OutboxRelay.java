package com.gk3.demo.orchestrator.outbox;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.events.EventEnvelope;
import com.gk3.demo.orchestrator.config.OutboxRelayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OutboxRelayProperties properties;

    public OutboxRelay(OutboxRepository outboxRepository, RestClient restClient, ObjectMapper objectMapper,
                        OutboxRelayProperties properties) {
        this.outboxRepository = outboxRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:2000}")
    public void relayPendingMessages() {
        List<OutboxMessage> batch = outboxRepository.findBatchOfPending(properties.batchSize());
        for (OutboxMessage message : batch) {
            deliver(message);
        }
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
            log.info("Delivered outbox message {} ({}) to {}", message.getId(), message.getEventType(), message.getTargetUrl());
        } catch (Exception ex) {
            message.markFailedAttempt(ex.getMessage(), properties.maxAttempts());
            log.warn("Failed to deliver outbox message {} ({}), attempt {}: {}",
                    message.getId(), message.getEventType(), message.getAttempts(), ex.getMessage());
        }
    }
}
