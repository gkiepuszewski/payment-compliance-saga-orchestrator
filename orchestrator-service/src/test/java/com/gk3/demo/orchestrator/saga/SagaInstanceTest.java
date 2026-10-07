package com.gk3.demo.orchestrator.saga;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SagaInstanceTest {

    @Test
    void startInitializesSagaKeyedByPaymentId() {
        UUID paymentId = UUID.randomUUID();

        SagaInstance saga = SagaInstance.start(paymentId, "alice", "bob", BigDecimal.TEN, "EUR");

        assertThat(saga.getId()).isEqualTo(paymentId);
        assertThat(saga.getState()).isEqualTo(SagaState.STARTED);
        assertThat(saga.getPayerId()).isEqualTo("alice");
        assertThat(saga.getPayeeId()).isEqualTo("bob");
        assertThat(saga.getDecisionReason()).isNull();
    }

    @Test
    void awaitingScreeningMovesSagaForward() {
        SagaInstance saga = SagaInstance.start(UUID.randomUUID(), "alice", "bob", BigDecimal.TEN, "EUR");

        saga.awaitingScreening();

        assertThat(saga.getState()).isEqualTo(SagaState.AWAITING_SCREENING);
    }

    @Test
    void completeMarksSagaAsCompleted() {
        SagaInstance saga = SagaInstance.start(UUID.randomUUID(), "alice", "bob", BigDecimal.TEN, "EUR");
        saga.awaitingScreening();

        saga.complete();

        assertThat(saga.getState()).isEqualTo(SagaState.COMPLETED);
    }

    @Test
    void compensateMarksSagaAsCompensatedWithReason() {
        SagaInstance saga = SagaInstance.start(UUID.randomUUID(), "alice", "bob", BigDecimal.TEN, "EUR");
        saga.awaitingScreening();

        saga.compensate("Matched sanctions list: alice");

        assertThat(saga.getState()).isEqualTo(SagaState.COMPENSATED);
        assertThat(saga.getDecisionReason()).isEqualTo("Matched sanctions list: alice");
    }
}
