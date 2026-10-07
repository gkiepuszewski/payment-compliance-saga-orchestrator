package com.gk3.demo.compliance.inbox;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.compliance.screening.ScreeningService;
import com.gk3.demo.events.EventEnvelope;
import com.gk3.demo.events.payload.ScreenPaymentCommandPayload;
import org.springframework.stereotype.Component;

@Component
public class ComplianceInboxDispatcher {

    private final ScreeningService screeningService;
    private final ObjectMapper objectMapper;

    public ComplianceInboxDispatcher(ScreeningService screeningService, ObjectMapper objectMapper) {
        this.screeningService = screeningService;
        this.objectMapper = objectMapper;
    }

    public void dispatch(EventEnvelope envelope) {
        switch (envelope.type()) {
            case SCREEN_PAYMENT_COMMAND -> {
                ScreenPaymentCommandPayload payload = objectMapper.convertValue(envelope.payload(), ScreenPaymentCommandPayload.class);
                screeningService.screen(payload);
            }
            default -> throw new IllegalArgumentException(
                    "compliance-service inbox does not support event type " + envelope.type());
        }
    }
}
