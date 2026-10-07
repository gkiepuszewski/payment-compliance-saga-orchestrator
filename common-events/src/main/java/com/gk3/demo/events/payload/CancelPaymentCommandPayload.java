package com.gk3.demo.events.payload;

import java.util.UUID;

/**
 * Sent by orchestrator-service to payment-service when screening was REJECTED: this is the saga's
 * compensating action, transitioning the payment from PENDING to CANCELLED.
 */
public record CancelPaymentCommandPayload(
        UUID paymentId,
        String reason
) {
}
