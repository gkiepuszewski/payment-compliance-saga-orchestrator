package com.gk3.demo.compliance.outbox;

import com.gk3.demo.events.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * See payment-service's {@code OutboxMessage} for the full rationale. Kept as a service-local
 * copy (not a shared library) so compliance-service stays an independently deployable unit.
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sagaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    protected OutboxMessage() {
        // JPA
    }

    public static OutboxMessage create(UUID sagaId, EventType eventType, String payloadJson, String targetUrl) {
        OutboxMessage message = new OutboxMessage();
        message.id = UUID.randomUUID();
        message.sagaId = sagaId;
        message.eventType = eventType;
        message.payloadJson = payloadJson;
        message.targetUrl = targetUrl;
        message.status = OutboxStatus.PENDING;
        message.attempts = 0;
        message.createdAt = Instant.now();
        return message;
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void markFailedAttempt(String error, int maxAttempts) {
        this.attempts++;
        this.lastError = error;
        if (this.attempts >= maxAttempts) {
            this.status = OutboxStatus.FAILED;
        }
    }

    /**
     * Re-queues a {@code FAILED} row for another delivery attempt (e.g. after an operator has
     * confirmed/fixed whatever caused it to exhaust its attempts). Resets the attempt counter so
     * it gets the full {@code maxAttempts} budget again.
     */
    public void resetForRetry() {
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.lastError = null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
