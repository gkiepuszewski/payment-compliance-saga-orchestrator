package com.gk3.demo.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String payerId;

    @Column(nullable = false)
    private String payeeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column
    private String cancelReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Payment() {
        // JPA
    }

    public static Payment createPending(String payerId, String payeeId, BigDecimal amount, String currency) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.payerId = payerId;
        payment.payeeId = payeeId;
        payment.amount = amount;
        payment.currency = currency;
        payment.status = PaymentStatus.PENDING;
        Instant now = Instant.now();
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    public void confirm() {
        if (status != PaymentStatus.PENDING) {
            return; // idempotent: command already applied (or payment in a terminal state)
        }
        this.status = PaymentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status != PaymentStatus.PENDING) {
            return; // idempotent: command already applied (or payment in a terminal state)
        }
        this.status = PaymentStatus.CANCELLED;
        this.cancelReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
