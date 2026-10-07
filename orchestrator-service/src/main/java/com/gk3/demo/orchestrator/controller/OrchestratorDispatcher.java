package com.gk3.demo.orchestrator.controller;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.events.EventEnvelope;
import com.gk3.demo.events.payload.PaymentCreatedPayload;
import com.gk3.demo.events.payload.PaymentScreenedPayload;
import com.gk3.demo.orchestrator.saga.SagaOrchestrationService;
import org.springframework.stereotype.Component;

@Component
public class OrchestratorDispatcher {

    private final SagaOrchestrationService sagaOrchestrationService;
    private final ObjectMapper objectMapper;

    public OrchestratorDispatcher(SagaOrchestrationService sagaOrchestrationService, ObjectMapper objectMapper) {
        this.sagaOrchestrationService = sagaOrchestrationService;
        this.objectMapper = objectMapper;
    }

    public void dispatch(EventEnvelope envelope) {
        switch (envelope.type()) {
            case PAYMENT_CREATED -> sagaOrchestrationService.onPaymentCreated(
                    objectMapper.convertValue(envelope.payload(), PaymentCreatedPayload.class));
            case PAYMENT_SCREENED -> sagaOrchestrationService.onPaymentScreened(
                    objectMapper.convertValue(envelope.payload(), PaymentScreenedPayload.class));
            default -> throw new IllegalArgumentException(
                    "orchestrator-service inbox does not support event type " + envelope.type());
        }
    }
}
