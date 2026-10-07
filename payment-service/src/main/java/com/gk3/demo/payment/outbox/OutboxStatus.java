package com.gk3.demo.payment.outbox;

/**
 * Delivery state of an {@link OutboxMessage}.
 */
public enum OutboxStatus {
    /** Waiting to be (re)sent by the relay. */
    PENDING,
    /** Target service acknowledged with 2xx; terminal state. */
    SENT,
    /** Exceeded max delivery attempts; requires manual inspection/replay. */
    FAILED
}
