package com.gk3.demo.orchestrator.api;

import com.gk3.demo.orchestrator.saga.SagaInstance;
import com.gk3.demo.orchestrator.saga.SagaState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SagaResponse(
        UUID paymentId,
        SagaState state,
        String payerId,
        String payeeId,
        BigDecimal amount,
        String currency,
        String decisionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static SagaResponse from(SagaInstance saga) {
        return new SagaResponse(
                saga.getId(), saga.getState(), saga.getPayerId(), saga.getPayeeId(), saga.getAmount(),
                saga.getCurrency(), saga.getDecisionReason(), saga.getCreatedAt(), saga.getUpdatedAt());
    }
}
