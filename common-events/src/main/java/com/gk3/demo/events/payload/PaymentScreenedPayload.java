package com.gk3.demo.events.payload;

import java.util.UUID;

/**
 * Published by compliance-service (via its outbox) once a screening decision has been made.
 * Consumed by orchestrator-service to drive the saga to completion (APPROVED) or compensation
 * (REJECTED).
 */
public record PaymentScreenedPayload(
        UUID paymentId,
        ScreeningDecision decision,
        String reason
) {
}
