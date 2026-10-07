package com.gk3.demo.events;

/**
 * Discriminator for {@link EventEnvelope#type()}, used by inbox handlers to decide how to
 * deserialize {@link EventEnvelope#payload()}.
 */
public enum EventType {
    PAYMENT_CREATED,
    SCREEN_PAYMENT_COMMAND,
    PAYMENT_SCREENED,
    CONFIRM_PAYMENT_COMMAND,
    CANCEL_PAYMENT_COMMAND
}
