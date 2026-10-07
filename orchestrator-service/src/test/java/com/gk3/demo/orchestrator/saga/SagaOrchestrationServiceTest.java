package com.gk3.demo.orchestrator.saga;

import com.gk3.demo.events.EventType;
import com.gk3.demo.events.payload.PaymentCreatedPayload;
import com.gk3.demo.events.payload.PaymentScreenedPayload;
import com.gk3.demo.events.payload.ScreeningDecision;
import com.gk3.demo.orchestrator.config.ComplianceServiceProperties;
import com.gk3.demo.orchestrator.config.PaymentServiceProperties;
import com.gk3.demo.orchestrator.outbox.OutboxMessage;
import com.gk3.demo.orchestrator.outbox.OutboxRepository;
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
class SagaOrchestrationServiceTest {

    private static final String PAYMENT_INBOX_URL = "http://payment-service:8081/api/payments/confirmation";
    private static final String COMPLIANCE_INBOX_URL = "http://compliance-service:8082/api/compliance/screening";

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private SagaOrchestrationService sagaOrchestrationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        sagaOrchestrationService = new SagaOrchestrationService(
                sagaRepository, outboxRepository,
                new PaymentServiceProperties(PAYMENT_INBOX_URL),
                new ComplianceServiceProperties(COMPLIANCE_INBOX_URL),
                objectMapper);
    }

    @Test
    void onPaymentCreatedStartsSagaAndRequestsScreening() {
        PaymentCreatedPayload payload = new PaymentCreatedPayload(
                UUID.randomUUID(), "alice", "bob", BigDecimal.TEN, "EUR");
        when(sagaRepository.existsById(payload.paymentId())).thenReturn(false);

        sagaOrchestrationService.onPaymentCreated(payload);

        ArgumentCaptor<SagaInstance> sagaCaptor = ArgumentCaptor.forClass(SagaInstance.class);
        verify(sagaRepository).save(sagaCaptor.capture());
        assertThat(sagaCaptor.getValue().getId()).isEqualTo(payload.paymentId());
        assertThat(sagaCaptor.getValue().getState()).isEqualTo(SagaState.AWAITING_SCREENING);

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.SCREEN_PAYMENT_COMMAND);
        assertThat(outboxCaptor.getValue().getTargetUrl()).isEqualTo(COMPLIANCE_INBOX_URL);
    }

    @Test
    void onPaymentCreatedIgnoresDuplicateForAlreadyStartedSaga() {
        PaymentCreatedPayload payload = new PaymentCreatedPayload(
                UUID.randomUUID(), "alice", "bob", BigDecimal.TEN, "EUR");
        when(sagaRepository.existsById(payload.paymentId())).thenReturn(true);

        sagaOrchestrationService.onPaymentCreated(payload);

        verify(sagaRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void onPaymentScreenedApprovedCompletesSagaAndConfirmsPayment() {
        UUID paymentId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.start(paymentId, "alice", "bob", BigDecimal.TEN, "EUR");
        saga.awaitingScreening();
        when(sagaRepository.findById(paymentId)).thenReturn(Optional.of(saga));

        sagaOrchestrationService.onPaymentScreened(new PaymentScreenedPayload(paymentId, ScreeningDecision.APPROVED, "No sanctions match"));

        assertThat(saga.getState()).isEqualTo(SagaState.COMPLETED);

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.CONFIRM_PAYMENT_COMMAND);
        assertThat(outboxCaptor.getValue().getTargetUrl()).isEqualTo(PAYMENT_INBOX_URL);
    }

    @Test
    void onPaymentScreenedRejectedCompensatesSagaAndCancelsPayment() {
        UUID paymentId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.start(paymentId, "alice", "bob", BigDecimal.TEN, "EUR");
        saga.awaitingScreening();
        when(sagaRepository.findById(paymentId)).thenReturn(Optional.of(saga));

        sagaOrchestrationService.onPaymentScreened(
                new PaymentScreenedPayload(paymentId, ScreeningDecision.REJECTED, "Matched sanctions list: alice"));

        assertThat(saga.getState()).isEqualTo(SagaState.COMPENSATED);
        assertThat(saga.getDecisionReason()).isEqualTo("Matched sanctions list: alice");

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(EventType.CANCEL_PAYMENT_COMMAND);
        assertThat(outboxCaptor.getValue().getTargetUrl()).isEqualTo(PAYMENT_INBOX_URL);
    }

    @Test
    void onPaymentScreenedIgnoresUnknownSaga() {
        UUID paymentId = UUID.randomUUID();
        when(sagaRepository.findById(paymentId)).thenReturn(Optional.empty());

        sagaOrchestrationService.onPaymentScreened(new PaymentScreenedPayload(paymentId, ScreeningDecision.APPROVED, "No sanctions match"));

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void onPaymentScreenedIgnoresDuplicateForAlreadyResolvedSaga() {
        UUID paymentId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.start(paymentId, "alice", "bob", BigDecimal.TEN, "EUR");
        saga.awaitingScreening();
        saga.complete();
        when(sagaRepository.findById(paymentId)).thenReturn(Optional.of(saga));

        sagaOrchestrationService.onPaymentScreened(new PaymentScreenedPayload(paymentId, ScreeningDecision.APPROVED, "No sanctions match"));

        verify(outboxRepository, never()).save(any());
    }
}
