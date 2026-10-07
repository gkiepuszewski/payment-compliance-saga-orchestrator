package com.gk3.demo.orchestrator.outbox;

import com.gk3.demo.orchestrator.config.OutboxRelayProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls PENDING outbox rows and delivers them over REST to their target service's Inbox endpoint.
 * Delivery is at-least-once: a non-2xx response or any exception simply leaves the row PENDING
 * (or FAILED after {@code maxAttempts}) so it is retried on the next tick. The receiving Inbox is
 * expected to dedupe by {@code messageId}, making retries safe. Actual delivery (and its
 * per-message {@code REQUIRES_NEW} transaction) is delegated to {@link OutboxMessageDelivery} -
 * see that class's javadoc for why it must be a separate Spring bean rather than a method here.
 */
@Component
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final OutboxMessageDelivery delivery;
    private final OutboxRelayProperties properties;

    public OutboxRelay(OutboxRepository outboxRepository, OutboxMessageDelivery delivery,
                        OutboxRelayProperties properties) {
        this.outboxRepository = outboxRepository;
        this.delivery = delivery;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:2000}")
    public void relayPendingMessages() {
        List<OutboxMessage> batch = outboxRepository.findBatchOfPending(properties.batchSize());
        for (OutboxMessage message : batch) {
            delivery.deliver(message);
        }
    }
}
