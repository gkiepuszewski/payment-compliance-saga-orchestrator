package com.gk3.demo.payment.domain;

import com.gk3.demo.events.EventType;
import com.gk3.demo.payment.config.OrchestratorProperties;
import com.gk3.demo.payment.outbox.OutboxMessage;
import com.gk3.demo.payment.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String ORCHESTRATOR_INBOX_URL = "http://orchestrator:8080/api/sagas/events";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        OrchestratorProperties orchestratorProperties = new OrchestratorProperties(ORCHESTRATOR_INBOX_URL);
        paymentService = new PaymentService(paymentRepository, outboxRepository, orchestratorProperties, objectMapper);
    }

    @Test
    void createPendingSavesPaymentAndEnqueuesPaymentCreatedOutboxMessage() {
        Payment payment = paymentService.createPending("alice", "bob", BigDecimal.TEN, "EUR");

        verify(paymentRepository).save(payment);

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxMessage outboxMessage = outboxCaptor.getValue();

        assertThat(outboxMessage.getSagaId()).isEqualTo(payment.getId());
        assertThat(outboxMessage.getEventType()).isEqualTo(EventType.PAYMENT_CREATED);
        assertThat(outboxMessage.getTargetUrl()).isEqualTo(ORCHESTRATOR_INBOX_URL);
        assertThat(outboxMessage.getPayloadJson())
                .contains(payment.getId().toString())
                .contains("alice")
                .contains("bob");
    }

    @Test
    void confirmAppliesCommandWhenPaymentExists() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        paymentService.confirm(payment.getId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void confirmIsNoOpWhenPaymentDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(paymentRepository.findById(unknownId)).thenReturn(Optional.empty());

        paymentService.confirm(unknownId);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void cancelAppliesCommandWithReasonWhenPaymentExists() {
        Payment payment = Payment.createPending("alice", "bob", BigDecimal.TEN, "EUR");
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        paymentService.cancel(payment.getId(), "Matched sanctions list: alice");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getCancelReason()).isEqualTo("Matched sanctions list: alice");
    }
}
