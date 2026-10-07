package com.gk3.demo.orchestrator.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted saga state. {@code id} is the {@code paymentId}, so there is exactly one saga
 * instance per payment.
 */
@Entity
@Table(name = "saga_instances")
public class SagaInstance {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SagaState state;

    @Column(nullable = false)
    private String payerId;

    @Column(nullable = false)
    private String payeeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column
    private String decisionReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SagaInstance() {
        // JPA
    }

    public static SagaInstance start(UUID paymentId, String payerId, String payeeId, BigDecimal amount, String currency) {
        SagaInstance saga = new SagaInstance();
        saga.id = paymentId;
        saga.state = SagaState.STARTED;
        saga.payerId = payerId;
        saga.payeeId = payeeId;
        saga.amount = amount;
        saga.currency = currency;
        Instant now = Instant.now();
        saga.createdAt = now;
        saga.updatedAt = now;
        return saga;
    }

    public void awaitingScreening() {
        this.state = SagaState.AWAITING_SCREENING;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.state = SagaState.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void compensate(String reason) {
        this.state = SagaState.COMPENSATED;
        this.decisionReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public SagaState getState() {
        return state;
    }

    public String getPayerId() {
        return payerId;
    }

    public String getPayeeId() {
        return payeeId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
