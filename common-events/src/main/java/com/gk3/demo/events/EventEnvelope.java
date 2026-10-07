package com.gk3.demo.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Transport envelope sent over REST from an Outbox relay to a peer service's Inbox endpoint.
 * <p>
 * {@code messageId} is the idempotency key: the receiving Inbox persists it before processing,
 * so redelivery (retries after timeouts/5xx) is safely ignored on the receiver side.
 *
 * @param messageId     stable id of the outbox row, used as the inbox dedup key
 * @param type          discriminator telling the receiver how to interpret {@code payload}
 * @param sourceService name of the service that produced this message (for tracing/debugging)
 * @param sagaId        correlation id of the saga instance this message belongs to
 * @param occurredAt    when the event/command was recorded in the sender's outbox
 * @param payload       event/command specific fields, as a generic map (re-materialized by the
 *                      receiver into a concrete payload record based on {@code type})
 */
public record EventEnvelope(
        UUID messageId,
        EventType type,
        String sourceService,
        UUID sagaId,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
