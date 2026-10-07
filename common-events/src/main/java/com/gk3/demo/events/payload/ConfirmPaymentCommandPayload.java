package com.gk3.demo.events.payload;

import java.util.UUID;

/**
 * Sent by orchestrator-service to payment-service when screening was APPROVED: transitions the
 * payment from PENDING to CONFIRMED (the saga's happy-path completion).
 */
public record ConfirmPaymentCommandPayload(
        UUID paymentId
) {
}
