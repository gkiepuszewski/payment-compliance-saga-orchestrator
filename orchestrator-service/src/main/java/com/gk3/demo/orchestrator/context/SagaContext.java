package com.gk3.demo.orchestrator.context;

import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

/**
 * Carries the current saga's correlation id through the call stack using a JDK 25 {@link ScopedValue}
 * (JEP 506, finalized in Java 25) instead of a mutable {@link ThreadLocal}.
 * <p>
 * Unlike a ThreadLocal, a ScopedValue is immutable for the dynamic extent of {@link #runWithSagaId},
 * cannot leak past it (no manual {@code remove()} to forget), and is safe to read from code deep in
 * the call stack - e.g. loggers, outbox writers - without passing {@code sagaId} as an explicit
 * parameter everywhere. It also composes correctly with virtual threads/structured concurrency,
 * which this service enables via {@code spring.threads.virtual.enabled=true}.
 */
public final class SagaContext {

    private static final ScopedValue<UUID> CURRENT_SAGA_ID = ScopedValue.newInstance();

    private SagaContext() {
    }

    /** Binds {@code sagaId} for the duration of {@code action} and mirrors it into the log MDC. */
    public static void runWithSagaId(UUID sagaId, Runnable action) {
        MDC.put("sagaId", String.valueOf(sagaId));
        try {
            ScopedValue.where(CURRENT_SAGA_ID, sagaId).run(action);
        } finally {
            MDC.remove("sagaId");
        }
    }

    /** Reads the saga id bound by the nearest enclosing {@link #runWithSagaId}, if any. */
    public static Optional<UUID> currentSagaId() {
        return CURRENT_SAGA_ID.isBound() ? Optional.of(CURRENT_SAGA_ID.get()) : Optional.empty();
    }
}
