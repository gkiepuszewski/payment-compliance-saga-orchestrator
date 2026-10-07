package com.gk3.demo.payment.domain;

/**
 * Lifecycle of a {@link Payment}. A payment is created as {@code PENDING} and leaves this state
 * exactly once, driven by a command from the saga orchestrator: {@code CONFIRMED} on approved
 * AML screening, {@code CANCELLED} (compensation) on rejected screening.
 */
public enum PaymentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
