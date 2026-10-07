package com.gk3.demo.events.payload;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sent by orchestrator-service to compliance-service, asking it to run an AML/sanctions
 * screening check for the given payment.
 */
public record ScreenPaymentCommandPayload(
        UUID paymentId,
        String payerId,
        String payeeId,
        BigDecimal amount,
        String currency
) {
}
