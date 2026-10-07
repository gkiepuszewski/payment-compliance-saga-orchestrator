package com.gk3.demo.compliance.inbox;

import com.gk3.demo.events.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_messages")
public class InboxMessage {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventType type;

    @Column(nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InboxStatus status;

    protected InboxMessage() {
        // JPA
    }

    public static InboxMessage receive(UUID messageId, EventType type) {
        InboxMessage inbox = new InboxMessage();
        inbox.id = messageId;
        inbox.type = type;
        inbox.receivedAt = Instant.now();
        inbox.status = InboxStatus.RECEIVED;
        return inbox;
    }

    public void markProcessed() {
        this.status = InboxStatus.PROCESSED;
    }

    public UUID getId() {
        return id;
    }

    public InboxStatus getStatus() {
        return status;
    }
}
