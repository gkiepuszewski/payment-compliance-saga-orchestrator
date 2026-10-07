package com.gk3.demo.events.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published by payment-service (via its outbox) right after a payment is persisted as PENDING.
 * Consumed by orchestrator-service, which starts a new saga instance keyed by {@code paymentId}.
 */
public record PaymentCreatedPayload(
        UUID paymentId,
        String payerId,
        String payeeId,
        BigDecimal amount,
        String currency
) {
}
