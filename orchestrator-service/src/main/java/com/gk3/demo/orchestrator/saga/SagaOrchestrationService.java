package com.gk3.demo.orchestrator.saga;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.events.EventType;
import com.gk3.demo.events.payload.CancelPaymentCommandPayload;
import com.gk3.demo.events.payload.ConfirmPaymentCommandPayload;
import com.gk3.demo.events.payload.PaymentCreatedPayload;
import com.gk3.demo.events.payload.PaymentScreenedPayload;
import com.gk3.demo.events.payload.ScreenPaymentCommandPayload;
import com.gk3.demo.orchestrator.config.ComplianceServiceProperties;
import com.gk3.demo.orchestrator.config.PaymentServiceProperties;
import com.gk3.demo.orchestrator.context.SagaContext;
import com.gk3.demo.orchestrator.outbox.OutboxMessage;
import com.gk3.demo.orchestrator.outbox.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The saga state machine. Each handler runs inside {@link SagaContext#runWithSagaId}, so the
 * saga id is available (via {@link SagaContext#currentSagaId()}) to every method it calls -
 * including {@link #log}, without being passed as an explicit parameter - which is the whole
 * point of using a ScopedValue instead of threading the id through the call graph manually.
 */
@Service
public class SagaOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrationService.class);

    private final SagaRepository sagaRepository;
    private final OutboxRepository outboxRepository;
    private final PaymentServiceProperties paymentServiceProperties;
    private final ComplianceServiceProperties complianceServiceProperties;
    private final ObjectMapper objectMapper;

    public SagaOrchestrationService(SagaRepository sagaRepository, OutboxRepository outboxRepository,
                                     PaymentServiceProperties paymentServiceProperties,
                                     ComplianceServiceProperties complianceServiceProperties,
                                     ObjectMapper objectMapper) {
        this.sagaRepository = sagaRepository;
        this.outboxRepository = outboxRepository;
        this.paymentServiceProperties = paymentServiceProperties;
        this.complianceServiceProperties = complianceServiceProperties;
        this.objectMapper = objectMapper;
    }

    /** Step 1: a new payment was created -> start the saga and ask compliance-service to screen it. */
    @Transactional
    public void onPaymentCreated(PaymentCreatedPayload payment) {
        if (sagaRepository.existsById(payment.paymentId())) {
            log("Saga already started for payment {}, ignoring duplicate PaymentCreated", payment.paymentId());
            return;
        }
        SagaInstance saga = SagaInstance.start(
                payment.paymentId(), payment.payerId(), payment.payeeId(), payment.amount(), payment.currency());
        saga.awaitingScreening();
        sagaRepository.save(saga);

        ScreenPaymentCommandPayload command = new ScreenPaymentCommandPayload(
                payment.paymentId(), payment.payerId(), payment.payeeId(), payment.amount(), payment.currency());
        enqueue(payment.paymentId(), EventType.SCREEN_PAYMENT_COMMAND, command, complianceServiceProperties.url());

        log("Saga started, requested AML screening for payment {}", payment.paymentId());
    }

    /** Step 2: compliance-service made a decision -> complete the saga or compensate the payment. */
    @Transactional
    public void onPaymentScreened(PaymentScreenedPayload screening) {
        SagaInstance saga = sagaRepository.findById(screening.paymentId()).orElse(null);
        if (saga == null) {
            log("Received PaymentScreened for unknown saga {}, ignoring", screening.paymentId());
            return;
        }
        if (saga.getState() != SagaState.AWAITING_SCREENING) {
            log("Saga {} already resolved ({}), ignoring duplicate PaymentScreened", screening.paymentId(), saga.getState());
            return;
        }

        switch (screening.decision()) {
            case APPROVED -> {
                saga.complete();
                enqueue(screening.paymentId(), EventType.CONFIRM_PAYMENT_COMMAND,
                        new ConfirmPaymentCommandPayload(screening.paymentId()), paymentServiceProperties.url());
                log("Screening APPROVED for payment {}, confirming payment", screening.paymentId());
            }
            case REJECTED -> {
                saga.compensate(screening.reason());
                enqueue(screening.paymentId(), EventType.CANCEL_PAYMENT_COMMAND,
                        new CancelPaymentCommandPayload(screening.paymentId(), screening.reason()), paymentServiceProperties.url());
                log("Screening REJECTED for payment {} ({}), compensating: cancelling payment",
                        screening.paymentId(), screening.reason());
            }
        }
    }

    private void enqueue(java.util.UUID sagaId, EventType type, Object payload, String targetUrl) {
        outboxRepository.save(OutboxMessage.create(sagaId, type, writeJson(payload), targetUrl));
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize outbox payload", ex);
        }
    }

    private void log(String message, Object... args) {
        // SagaContext.currentSagaId() is read here purely from the ScopedValue, with no sagaId
        // parameter on this method - demonstrating the propagation JEP 506 scoped values provide.
        log.info("[{}] " + message, prepend(SagaContext.currentSagaId().orElse(null), args));
    }

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] all = new Object[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }
}
