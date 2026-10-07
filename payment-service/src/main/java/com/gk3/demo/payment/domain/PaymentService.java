package com.gk3.demo.payment.domain;

import tools.jackson.databind.ObjectMapper;
import com.gk3.demo.events.EventType;
import com.gk3.demo.events.payload.PaymentCreatedPayload;
import com.gk3.demo.payment.config.OrchestratorProperties;
import com.gk3.demo.payment.outbox.OutboxMessage;
import com.gk3.demo.payment.outbox.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final OrchestratorProperties orchestratorProperties;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository, OutboxRepository outboxRepository,
                           OrchestratorProperties orchestratorProperties, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.orchestratorProperties = orchestratorProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Payment createPending(String payerId, String payeeId, BigDecimal amount, String currency) {
        Payment payment = Payment.createPending(payerId, payeeId, amount, currency);
        paymentRepository.save(payment);

        // Written in the SAME local transaction as the Payment insert: either both are committed
        // or neither is. The OutboxRelay delivers this row asynchronously, so orchestrator-service
        // never has to be called synchronously from this request.
        PaymentCreatedPayload payload = new PaymentCreatedPayload(
                payment.getId(), payment.getPayerId(), payment.getPayeeId(), payment.getAmount(), payment.getCurrency());
        String payloadJson = writeJson(payload);
        outboxRepository.save(OutboxMessage.create(
                payment.getId(), EventType.PAYMENT_CREATED, payloadJson, orchestratorProperties.url()));

        return payment;
    }

    @Transactional
    public void confirm(UUID paymentId) {
        paymentRepository.findById(paymentId).ifPresent(Payment::confirm);
    }

    @Transactional
    public void cancel(UUID paymentId, String reason) {
        paymentRepository.findById(paymentId).ifPresent(payment -> payment.cancel(reason));
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize outbox payload", ex);
        }
    }
}
