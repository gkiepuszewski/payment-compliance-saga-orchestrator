package com.gk3.demo.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    void createPendingInitializesAggregateInPendingState() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getPayerId()).isEqualTo("alice");
        assertThat(payment.getPayeeId()).isEqualTo("bob");
        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(payment.getCurrency()).isEqualTo("EUR");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCancelReason()).isNull();
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isEqualTo(payment.getCreatedAt());
    }

    @Test
    void confirmTransitionsPendingPaymentToConfirmed() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void confirmIsIdempotentOnceAlreadyConfirmed() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");
        payment.confirm();
        var updatedAtAfterFirstConfirm = payment.getUpdatedAt();

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getUpdatedAt()).isEqualTo(updatedAtAfterFirstConfirm);
    }

    @Test
    void confirmIsNoOpOnceCancelled() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");
        payment.cancel("Matched sanctions list: alice");

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void cancelTransitionsPendingPaymentToCancelledWithReason() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");

        payment.cancel("Matched sanctions list: alice");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getCancelReason()).isEqualTo("Matched sanctions list: alice");
    }

    @Test
    void cancelIsNoOpOnceConfirmed() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");
        payment.confirm();

        payment.cancel("too late");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(payment.getCancelReason()).isNull();
    }
}
