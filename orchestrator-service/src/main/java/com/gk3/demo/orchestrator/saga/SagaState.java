package com.gk3.demo.orchestrator.saga;

/**
 * States of the Payment + Compliance saga, keyed by {@code paymentId}.
 * <pre>
 *   STARTED --(ScreenPaymentCommand sent)--&gt; AWAITING_SCREENING
 *   AWAITING_SCREENING --(PaymentScreened: APPROVED, ConfirmPaymentCommand sent)--&gt; COMPLETED
 *   AWAITING_SCREENING --(PaymentScreened: REJECTED, CancelPaymentCommand sent)--&gt; COMPENSATED
 * </pre>
 */
public enum SagaState {
    STARTED,
    AWAITING_SCREENING,
    COMPLETED,
    COMPENSATED
}
