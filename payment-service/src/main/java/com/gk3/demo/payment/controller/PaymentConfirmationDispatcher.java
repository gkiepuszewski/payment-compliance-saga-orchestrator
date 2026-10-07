package com.gk3.demo.payment.controller;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.events.EventEnvelope;
import com.gk3.demo.events.payload.CancelPaymentCommandPayload;
import com.gk3.demo.events.payload.ConfirmPaymentCommandPayload;
import com.gk3.demo.payment.domain.PaymentService;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmationDispatcher {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentConfirmationDispatcher(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    public void dispatch(EventEnvelope envelope) {
        switch (envelope.type()) {
            case CONFIRM_PAYMENT_COMMAND -> {
                ConfirmPaymentCommandPayload payload = convert(envelope, ConfirmPaymentCommandPayload.class);
                paymentService.confirm(payload.paymentId());
            }
            case CANCEL_PAYMENT_COMMAND -> {
                CancelPaymentCommandPayload payload = convert(envelope, CancelPaymentCommandPayload.class);
                paymentService.cancel(payload.paymentId(), payload.reason());
            }
            default -> throw new IllegalArgumentException(
                    "payment-service inbox does not support event type " + envelope.type());
        }
    }

    private <T> T convert(EventEnvelope envelope, Class<T> type) {
        return objectMapper.convertValue(envelope.payload(), type);
    }
}
