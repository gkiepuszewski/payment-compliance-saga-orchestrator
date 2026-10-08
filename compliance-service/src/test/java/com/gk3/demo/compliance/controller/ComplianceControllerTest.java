package com.gk3.demo.compliance.controller;

import com.gk3.demo.compliance.inbox.InboxMessage;
import com.gk3.demo.compliance.inbox.InboxRepository;
import com.gk3.demo.events.EventEnvelope;
import com.gk3.demo.events.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceControllerTest {

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private ComplianceDispatcher dispatcher;

    private ComplianceController controller;

    @BeforeEach
    void setUp() {
        controller = new ComplianceController(inboxRepository, dispatcher);
    }

    @Test
    void dispatchesAndMarksMessageProcessedOnFirstDelivery() {
        EventEnvelope envelope = envelope();
        when(inboxRepository.existsById(envelope.messageId())).thenReturn(false);

        ResponseEntity<Void> response = controller.receive(envelope);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(dispatcher).dispatch(envelope);
        ArgumentCaptor<InboxMessage> captor = ArgumentCaptor.forClass(InboxMessage.class);
        verify(inboxRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(envelope.messageId());
    }

    @Test
    void ignoresAlreadyProcessedMessage() {
        EventEnvelope envelope = envelope();
        when(inboxRepository.existsById(envelope.messageId())).thenReturn(true);

        ResponseEntity<Void> response = controller.receive(envelope);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(inboxRepository, never()).save(any());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void returnsOkWhenConcurrentDuplicateDeliveryDetected() {
        EventEnvelope envelope = envelope();
        when(inboxRepository.existsById(envelope.messageId())).thenReturn(false);
        when(inboxRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        ResponseEntity<Void> response = controller.receive(envelope);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verifyNoInteractions(dispatcher);
    }

    @Test
    void propagatesExceptionsFromDispatcher() {
        EventEnvelope envelope = envelope();
        when(inboxRepository.existsById(envelope.messageId())).thenReturn(false);
        doThrow(new IllegalArgumentException("unsupported event type")).when(dispatcher).dispatch(envelope);

        assertThrows(IllegalArgumentException.class, () -> controller.receive(envelope));
    }

    private static EventEnvelope envelope() {
        return new EventEnvelope(
                UUID.randomUUID(),
                EventType.SCREEN_PAYMENT_COMMAND,
                "payment-service",
                UUID.randomUUID(),
                Instant.now(),
                Map.of("paymentId", UUID.randomUUID().toString()));
    }
}
